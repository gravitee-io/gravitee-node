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

import static org.junit.Assert.*;

import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesKeyStoreLoaderFactoryTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesKeyStoreLoaderFactory cut;

    @Before
    public void before() {
        cut = new KubernetesKeyStoreLoaderFactory(kubernetesClient);
    }

    @Test
    public void shouldHandleOptionsWithConfigMap() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .build();

        assertTrue(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithConfigMapAndPEMFormat() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .build();

        assertFalse(cut.canHandle(options));
    }

    @Test
    public void shouldHandleOptionsWithTlsSecret() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-secret"))
            .build();

        assertTrue(cut.canHandle(options));
    }

    @Test
    public void shouldHandleOptionsWithOpaqueSecret() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-secret/keystore"))
            .build();

        assertTrue(cut.canHandle(options));
    }

    @Test
    public void shouldCreateConfigMapLoader() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .build();

        assertEquals(KubernetesConfigMapKeyStoreLoader.class, cut.create(options).getClass());
    }

    @Test
    public void shouldCreateSecretLoader() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-secret"))
            .build();

        assertEquals(KubernetesSecretKeyStoreLoader.class, cut.create(options).getClass());
    }

    @Test
    public void shouldNotHandleOptionsWithoutKubernetesLocations() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.emptyList())
            .build();

        assertFalse(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithoutUnsupportedLocation() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/unknown"))
            .build();

        assertFalse(cut.canHandle(options));
    }
}
