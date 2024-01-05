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

import static io.gravitee.node.kubernetes.keystoreloader.KubernetesPemRegistryKeyStoreLoader.GRAVITEEIO_PEM_REGISTRY_LABEL;
import static io.gravitee.node.kubernetes.keystoreloader.KubernetesSecretKeyStoreLoader.*;
import static io.gravitee.node.kubernetes.keystoreloader.KubernetesSecretKeyStoreLoader.KUBERNETES_TLS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.model.v1.*;
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.rxjava3.core.Maybe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
public class KubernetesConfigMapKeyStoreLoaderTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesConfigMapKeyStoreLoader cut;

    @Test
    public void shouldLoadConfigMap() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .withKeyStorePassword("secret")
            .withWatch(false)
            .build();

        cut = new KubernetesConfigMapKeyStoreLoader(options, kubernetesClient);

        final ConfigMap configMap = new ConfigMap();

        final HashMap<String, String> data = new HashMap<>();
        data.put("keystore", readContent("localhost.p12"));
        configMap.setBinaryData(data);

        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName("my-configmap");
        metadata.setUid("/namespaces/gio/configmaps/my-configmap");
        metadata.setNamespace("gio");
        configMap.setMetadata(metadata);

        Mockito
            .when(kubernetesClient.get(ResourceQuery.<ConfigMap>from("/gio/configmaps/my-configmap").build()))
            .thenReturn(Maybe.just(configMap));

        AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);
        cut.start();

        final KeyStoreBundle keyStoreBundle = bundleRef.get();

        assertNotNull(keyStoreBundle);
        assertEquals(1, keyStoreBundle.getKeyStore().size());
    }

    @Test
    public void shouldLoadConfigMapFromData() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .withKeyStorePassword("secret")
            .withWatch(false)
            .build();

        cut = new KubernetesConfigMapKeyStoreLoader(options, kubernetesClient);

        final ConfigMap configMap = new ConfigMap();

        final HashMap<String, String> data = new HashMap<>();
        data.put("keystore", readContent("localhost.p12"));
        configMap.setData(data);

        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName("my-configmap");
        metadata.setUid("/namespaces/gio/configmaps/my-configmap");
        metadata.setNamespace("gio");
        configMap.setMetadata(metadata);

        Mockito
            .when(kubernetesClient.get(ResourceQuery.<ConfigMap>from("/gio/configmaps/my-configmap").build()))
            .thenReturn(Maybe.just(configMap));

        AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);
        cut.start();

        final KeyStoreBundle keyStoreBundle = bundleRef.get();

        assertNotNull(keyStoreBundle);
        assertEquals(1, keyStoreBundle.getKeyStore().size());
    }

    private String readContent(String resource) throws IOException {
        return java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(new File(getPath(resource)).toPath()));
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }
}
