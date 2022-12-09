/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.kubernetes.keystoreloader;

import static io.gravitee.node.kubernetes.keystoreloader.KubernetesSecretKeyStoreLoader.*;
import static org.junit.Assert.*;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.model.v1.ObjectMeta;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.kubernetes.client.model.v1.SecretEvent;
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesSecretKeyStoreLoaderTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesSecretKeyStoreLoader cut;

    @Test
    public void shouldLoadTlsSecret() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .withKubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret"))
            .withWatch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType(KUBERNETES_TLS_SECRET);

        final HashMap<String, String> data = new HashMap<>();
        data.put(KUBERNETES_TLS_CRT, readContent("localhost.cer"));
        data.put(KUBERNETES_TLS_KEY, readContent("localhost.key"));
        secret.setData(data);

        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName("my-tls-secret");
        metadata.setUid("/namespaces/gio/secrets/my-tls-secret");
        secret.setMetadata(metadata);

        Mockito
            .when(kubernetesClient.get(ResourceQuery.<Secret>from(options.getKubernetesLocations().get(0)).build()))
            .thenReturn(Maybe.just(secret));

        AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);
        cut.start();

        final KeyStoreBundle keyStoreBundle = bundleRef.get();

        assertNotNull(keyStoreBundle);
        assertEquals(1, keyStoreBundle.getKeyStore().size());
    }

    @Test
    public void shouldLoadOpaqueSecret() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret/keystore"))
            .withKeyStorePassword("secret")
            .withWatch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType(KUBERNETES_OPAQUE_SECRET);

        final HashMap<String, String> data = new HashMap<>();
        data.put("keystore", readContent("localhost.p12"));
        secret.setData(data);

        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName("my-tls-secret");
        metadata.setNamespace("gio");
        metadata.setUid("/namespaces/gio/secrets/my-tls-secret");
        secret.setMetadata(metadata);

        Mockito.when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret").build())).thenReturn(Maybe.just(secret));

        AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);
        cut.start();

        final KeyStoreBundle keyStoreBundle = bundleRef.get();

        assertNotNull(keyStoreBundle);
        assertEquals(1, keyStoreBundle.getKeyStore().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotLoadOpaqueSecretPEM() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .withKubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret/pem"))
            .withKeyStorePassword("secret")
            .withWatch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType(KUBERNETES_OPAQUE_SECRET);

        Mockito.when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret").build())).thenReturn(Maybe.just(secret));

        cut.start();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotLoadOpaqueSecretWithNoDataKey() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret"))
            .withKeyStorePassword("secret")
            .withWatch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType(KUBERNETES_OPAQUE_SECRET);

        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName("my-tls-secret");
        metadata.setUid("/namespaces/gio/secrets/my-tls-secret");
        secret.setMetadata(metadata);

        Mockito.when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret").build())).thenReturn(Maybe.just(secret));

        cut.start();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotLoadWithInvalidSecretType() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret/invalid-keystore"))
            .withKeyStorePassword("secret")
            .withWatch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType("Invalid");

        Mockito.when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret").build())).thenReturn(Maybe.just(secret));

        cut.start();
    }

    @Test
    public void shouldWatchSecret() throws IOException, KeyStoreException, InterruptedException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .withKubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret"))
            .withWatch(true)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType(KUBERNETES_TLS_SECRET);

        final HashMap<String, String> data = new HashMap<>();
        data.put(KUBERNETES_TLS_CRT, readContent("localhost.cer"));
        data.put(KUBERNETES_TLS_KEY, readContent("localhost.key"));
        secret.setData(data);

        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName("my-tls-secret");
        metadata.setUid("/namespaces/gio/secrets/my-tls-secret");
        secret.setMetadata(metadata);

        final Secret modifiedSecret = new Secret();
        modifiedSecret.setType(KUBERNETES_TLS_SECRET);

        final HashMap<String, String> modifiedData = new HashMap<>();
        modifiedData.put(KUBERNETES_TLS_CRT, readContent("localhost2.cer"));
        modifiedData.put(KUBERNETES_TLS_KEY, readContent("localhost2.key"));
        modifiedSecret.setData(modifiedData);

        final ObjectMeta modifiedMetadata = new ObjectMeta();
        modifiedMetadata.setName("my-tls-secret");
        modifiedMetadata.setUid("/namespaces/gio/secrets/my-tls-secret");
        modifiedSecret.setMetadata(modifiedMetadata);

        final SecretEvent modifiedSecretEvent = new SecretEvent();
        modifiedSecretEvent.setType("MODIFIED");
        modifiedSecretEvent.setObject(modifiedSecret);

        Mockito
            .when(kubernetesClient.get(ResourceQuery.<Secret>from(options.getKubernetesLocations().get(0)).build()))
            .thenReturn(Maybe.just(secret));
        Mockito
            .when(kubernetesClient.watch(WatchQuery.<Secret>from(options.getKubernetesLocations().get(0)).build()))
            .thenReturn(Flowable.just(modifiedSecretEvent));

        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundle -> {
            bundleRef.set(bundle);
            latch.countDown();
        });

        cut.start();

        // Wait for the event to be processed.
        latch.await(5000, TimeUnit.MILLISECONDS);
        final KeyStoreBundle keyStoreBundle = bundleRef.get();

        assertNotNull(keyStoreBundle);
        assertEquals(1, keyStoreBundle.getKeyStore().size());
        // Make sure the keystore has been reloaded by checking the CN.
        assertTrue(KeyStoreUtils.getCommonNamesByAlias(keyStoreBundle.getKeyStore()).containsKey("localhost2"));
    }

    private String readContent(String resource) throws IOException {
        return java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(new File(getPath(resource)).toPath()));
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }
}
