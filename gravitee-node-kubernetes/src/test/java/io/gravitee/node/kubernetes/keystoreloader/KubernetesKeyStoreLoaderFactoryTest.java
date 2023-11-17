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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KubernetesKeyStoreLoaderFactoryTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesKeyStoreLoaderFactory cut;

    @BeforeEach
    public void before() {
        cut = new KubernetesKeyStoreLoaderFactory(kubernetesClient);
    }

    @Test
    void should_handle_options_with_config_map() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .build();

        assertThat(cut.canHandle(options)).isTrue();
    }

    @Test
    void should_not_handle_options_with_config_map_and_pemformat() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .build();

        assertThat(cut.canHandle(options)).isFalse();
    }

    @Test
    void should_handle_options_with_tls_secret() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-secret"))
            .build();

        assertThat(cut.canHandle(options)).isTrue();
    }

    @Test
    void should_handle_options_with_opaque_secret() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-secret/keystore"))
            .build();

        assertThat(cut.canHandle(options)).isTrue();
    }

    @Test
    void should_create_config_map_loader() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/configmaps/my-configmap/keystore"))
            .build();

        assertThat(cut.create(options).getClass()).isEqualTo(KubernetesConfigMapKeyStoreLoader.class);
    }

    @Test
    void should_create_secret_loader() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .kubernetesLocations(Collections.singletonList("/gio/secrets/my-secret"))
            .build();

        assertThat(cut.create(options).getClass()).isEqualTo(KubernetesSecretKeyStoreLoader.class);
    }

    @Test
    void should_not_handle_options_without_kubernetes_locations() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.emptyList())
            .build();

        assertThat(cut.canHandle(options)).isFalse();
    }

    @Test
    void should_not_handle_options_without_unsupported_location() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .kubernetesLocations(Collections.singletonList("/gio/unknown"))
            .build();

        assertThat(cut.canHandle(options)).isFalse();
    }
}
