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

import static io.gravitee.node.api.certificate.KeyStoreLoader.GRAVITEEIO_PEM_REGISTRY;
import static io.gravitee.node.kubernetes.keystoreloader.KubernetesSecretKeyStoreLoader.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.ObjectMeta;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.rxjava3.core.Maybe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
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
class KubernetesConfigMapKeyStoreLoaderTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesConfigMapKeyStoreLoader cut;

    @Test
    void should_load_config_map() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .password("secret")
            .watch(false)
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

        AtomicReference<KeyStoreEvent> bundleRef = new AtomicReference<>(null);
        cut.setEventHandler(bundleRef::set);
        cut.start();

        final KeyStoreEvent keyStoreEvent = bundleRef.get();

        assertNotNull(keyStoreEvent);
        assertEquals(1, keyStoreEvent.keyStore().size());
    }

    @Test
    void should_load_config_map_from_data() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .password("secret")
            .watch(false)
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

        AtomicReference<KeyStoreEvent> bundleRef = new AtomicReference<>(null);
        cut.setEventHandler(bundleRef::set);
        cut.start();

        final KeyStoreEvent keyStoreEvent = bundleRef.get();

        assertNotNull(keyStoreEvent);
        assertEquals(1, keyStoreEvent.keyStore().size());
    }

    @Test
    void should_load_gravitee_pem_registry() throws IOException, KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_REGISTRY)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/" + KeyStoreLoader.GRAVITEEIO_PEM_REGISTRY))
            .watch(false)
            .build();

        cut = new KubernetesConfigMapKeyStoreLoader(options, kubernetesClient);

        final ConfigMap pemRegistry = new ConfigMap();

        final HashMap<String, String> pemRegistryData = new HashMap<>();
        pemRegistryData.put("localhost", "my-tls-secret1");
        pemRegistryData.put("localhost2", "my-tls-secret2");
        pemRegistry.setData(pemRegistryData);

        final ObjectMeta pemRegistryMetadata = new ObjectMeta();
        pemRegistryMetadata.setName(KeyStoreLoader.GRAVITEEIO_PEM_REGISTRY);
        pemRegistryMetadata.setNamespace("gio");
        pemRegistryMetadata.setAnnotations(Collections.singletonMap(GRAVITEEIO_PEM_REGISTRY, "true"));
        pemRegistryMetadata.setUid("/namespaces/gio/configmaps" + KeyStoreLoader.GRAVITEEIO_PEM_REGISTRY);
        pemRegistry.setMetadata(pemRegistryMetadata);

        final Secret secret1 = new Secret();
        secret1.setType(KUBERNETES_TLS_SECRET);

        final HashMap<String, String> data1 = new HashMap<>();
        data1.put(KUBERNETES_TLS_CRT, readContent("localhost.cer"));
        data1.put(KUBERNETES_TLS_KEY, readContent("localhost.key"));
        secret1.setData(data1);

        final ObjectMeta metadata1 = new ObjectMeta();
        metadata1.setName("my-tls-secret1");
        metadata1.setUid("/namespaces/gio/secrets/my-tls-secret");
        secret1.setMetadata(metadata1);

        final Secret secret2 = new Secret();
        secret2.setType(KUBERNETES_TLS_SECRET);

        final HashMap<String, String> data2 = new HashMap<>();
        data2.put(KUBERNETES_TLS_CRT, readContent("localhost2.cer"));
        data2.put(KUBERNETES_TLS_KEY, readContent("localhost2.key"));
        secret2.setData(data2);

        final ObjectMeta metadata2 = new ObjectMeta();
        metadata2.setName("my-tls-secret2");
        metadata2.setUid("/namespaces/gio/secrets/my-tls-secret2");
        secret2.setMetadata(metadata2);

        Mockito
            .when(kubernetesClient.get(ResourceQuery.<ConfigMap>from("/gio/configmaps/" + KeyStoreLoader.GRAVITEEIO_PEM_REGISTRY).build()))
            .thenReturn((Maybe.just(pemRegistry)));
        Mockito
            .when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret1").build()))
            .thenReturn(Maybe.just(secret1));
        Mockito
            .when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret2").build()))
            .thenReturn(Maybe.just(secret2));

        AtomicReference<KeyStoreEvent> bundleRef = new AtomicReference<>(null);
        cut.setEventHandler(bundleRef::set);
        cut.start();

        final KeyStoreEvent keyStoreEvent = bundleRef.get();

        assertNotNull(keyStoreEvent);
        assertEquals(2, keyStoreEvent.keyStore().size());
    }

    private String readContent(String resource) throws IOException {
        return java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(new File(getPath(resource)).toPath()));
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }
}
