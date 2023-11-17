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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.model.v1.ObjectMeta;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.kubernetes.client.model.v1.SecretEvent;
import io.gravitee.node.api.certificate.KeyStoreEvent;
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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KubernetesSecretKeyStoreLoaderTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesSecretKeyStoreLoader cut;

    @Test
    void should_load_tls_secret() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret"))
            .watch(false)
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

        AtomicReference<KeyStoreEvent> bundleRef = new AtomicReference<>(null);
        cut.setEventHandler(bundleRef::set);
        cut.start();

        final KeyStoreEvent keyStoreEvent = bundleRef.get();

        assertThat(keyStoreEvent).isNotNull();
        assertThat(((KeyStoreEvent.LoadEvent) keyStoreEvent).keyStore().size()).isEqualTo(1);
    }

    @Test
    void should_load_opaque_secret() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret/keystore"))
            .password("secret")
            .watch(false)
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

        AtomicReference<KeyStoreEvent> bundleRef = new AtomicReference<>(null);
        cut.setEventHandler(bundleRef::set);
        cut.start();

        final KeyStoreEvent keyStoreEvent = bundleRef.get();

        assertThat(keyStoreEvent).isNotNull();
        assertThat(((KeyStoreEvent.LoadEvent) keyStoreEvent).keyStore().size()).isEqualTo(1);
    }

    @Test
    void should_not_load_opaque_secret_pem() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret/pem"))
            .password("secret")
            .watch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType(KUBERNETES_OPAQUE_SECRET);

        Mockito.when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret").build())).thenReturn(Maybe.just(secret));

        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_not_load_opaque_secret_with_no_data_key() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret"))
            .password("secret")
            .watch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType(KUBERNETES_OPAQUE_SECRET);

        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName("my-tls-secret");
        metadata.setUid("/namespaces/gio/secrets/my-tls-secret");
        secret.setMetadata(metadata);

        Mockito.when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret").build())).thenReturn(Maybe.just(secret));

        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_not_load_with_invalid_secret_type() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret/invalid-keystore"))
            .password("secret")
            .watch(false)
            .build();

        cut = new KubernetesSecretKeyStoreLoader(options, kubernetesClient);

        final Secret secret = new Secret();
        secret.setType("Invalid");

        Mockito.when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret").build())).thenReturn(Maybe.just(secret));

        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_watch_secret() throws IOException, KeyStoreException, InterruptedException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-tls-secret"))
            .watch(true)
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
        AtomicReference<KeyStoreEvent> bundleRef = new AtomicReference<>(null);
        cut.setEventHandler(bundle -> {
            bundleRef.set(bundle);
            latch.countDown();
        });

        cut.start();

        // Wait for the event to be processed.
        latch.await(5000, TimeUnit.MILLISECONDS);
        final KeyStoreEvent keyStoreEvent = bundleRef.get();

        assertThat(keyStoreEvent).isNotNull();
        assertThat(((KeyStoreEvent.LoadEvent) keyStoreEvent).keyStore().size()).isEqualTo(1);
        // Make sure the keystore has been reloaded by checking the CN.
        assertThat(KeyStoreUtils.getCommonNamesByAlias(((KeyStoreEvent.LoadEvent) keyStoreEvent).keyStore())).containsKey("localhost2");
    }

    private String readContent(String resource) throws IOException {
        return java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(new File(getPath(resource)).toPath()));
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }
}
