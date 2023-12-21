package io.gravitee.node.kubernetes.keystoreloader;

import static io.gravitee.node.api.certificate.KeyStoreLoader.CERTIFICATE_FORMAT_PEM_REGISTRY;
import static io.gravitee.node.kubernetes.keystoreloader.KubernetesPemRegistryKeyStoreLoader.GRAVITEEIO_PEM_REGISTRY_LABEL;
import static io.gravitee.node.kubernetes.keystoreloader.KubernetesSecretKeyStoreLoader.*;
import static io.gravitee.node.kubernetes.keystoreloader.KubernetesSecretKeyStoreLoader.KUBERNETES_TLS_KEY;
import static org.junit.Assert.*;

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
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesPemRegistryKeyStoreLoaderTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesPemRegistryKeyStoreLoader cut;

    @Test
    public void shouldLoadGraviteePemRegistry() throws IOException, KeyStoreException {
        KubernetesConfig.getInstance().setCurrentNamespace("test");
        final ConfigMap pemRegistry = new ConfigMap();

        final HashMap<String, String> pemRegistryData = new HashMap<>();
        pemRegistryData.put("site1", "[\"gio/my-tls-secret1\",\"gio/my-tls-secret2\"]");
        pemRegistry.setData(pemRegistryData);

        final ObjectMeta pemRegistryMetadata = new ObjectMeta();
        pemRegistryMetadata.setName(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_REGISTRY);
        pemRegistryMetadata.setNamespace("gio");
        pemRegistryMetadata.setLabels(Collections.singletonMap(GRAVITEEIO_PEM_REGISTRY_LABEL, CERTIFICATE_FORMAT_PEM_REGISTRY));
        pemRegistryMetadata.setUid("/namespaces/gio/configmaps/" + GRAVITEEIO_PEM_REGISTRY_LABEL);
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
            .when(
                kubernetesClient.get(
                    ResourceQuery
                        .configMaps("test")
                        .labelSelector(LabelSelector.equals(GRAVITEEIO_PEM_REGISTRY_LABEL, CERTIFICATE_FORMAT_PEM_REGISTRY))
                        .build()
                )
            )
            .thenReturn(Maybe.just(new ConfigMapList("v1", List.of(pemRegistry), "v1", new ListMeta("1", 1L, "1234", "/selflink"))));
        Mockito
            .when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret1").build()))
            .thenReturn(Maybe.just(secret1));
        Mockito
            .when(kubernetesClient.get(ResourceQuery.<Secret>from("/gio/secrets/my-tls-secret2").build()))
            .thenReturn(Maybe.just(secret2));

        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_REGISTRY)
            .withKeyStorePassword("secret")
            .withWatch(false)
            .build();

        cut = new KubernetesPemRegistryKeyStoreLoader(options, kubernetesClient);

        AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);
        cut.start();

        final KeyStoreBundle keyStoreBundle = bundleRef.get();

        assertNotNull(keyStoreBundle);
        assertEquals(2, keyStoreBundle.getKeyStore().size());
    }

    private String readContent(String resource) throws IOException {
        return java.util.Base64
            .getEncoder()
            .encodeToString(Files.readAllBytes(new File(this.getClass().getResource("/keystores/" + resource).getPath()).toPath()));
    }
}
