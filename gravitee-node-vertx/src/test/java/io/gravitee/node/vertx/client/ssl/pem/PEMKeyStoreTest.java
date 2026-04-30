/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.vertx.client.ssl.pem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.vertx.core.net.PemKeyCertOptions;
import java.util.List;
import org.junit.jupiter.api.Test;

class PEMKeyStoreTest {

    @Test
    void shouldBuildKeyCertOptionsFromMultiCertList() {
        var keyStore = PEMKeyStore.builder().certPaths(List.of("cert1.pem", "cert2.pem")).keyPaths(List.of("key1.pem", "key2.pem")).build();

        var keyCertOptions = (PemKeyCertOptions) keyStore.keyCertOptions().orElseThrow();

        assertThat(keyCertOptions.getCertPaths()).containsExactly("cert1.pem", "cert2.pem");
        assertThat(keyCertOptions.getKeyPaths()).containsExactly("key1.pem", "key2.pem");
    }

    @Test
    void shouldPreferMultiCertListOverSingleCertFields() {
        var keyStore = PEMKeyStore
            .builder()
            .certPath("ignored.pem")
            .keyPath("ignored-key.pem")
            .certPaths(List.of("cert1.pem"))
            .keyPaths(List.of("key1.pem"))
            .build();

        var keyCertOptions = (PemKeyCertOptions) keyStore.keyCertOptions().orElseThrow();

        assertThat(keyCertOptions.getCertPaths()).containsExactly("cert1.pem").doesNotContain("ignored.pem");
        assertThat(keyCertOptions.getKeyPaths()).containsExactly("key1.pem").doesNotContain("ignored-key.pem");
    }

    @Test
    void shouldFallBackToSingleCertWhenListsAreNull() {
        var keyStore = PEMKeyStore.builder().certPath("cert.pem").keyPath("key.pem").build();

        var keyCertOptions = (PemKeyCertOptions) keyStore.keyCertOptions().orElseThrow();

        assertThat(keyCertOptions.getCertPath()).isEqualTo("cert.pem");
        assertThat(keyCertOptions.getKeyPath()).isEqualTo("key.pem");
    }

    @Test
    void shouldRejectMismatchedCertAndKeyListSizes() {
        var keyStore = PEMKeyStore.builder().certPaths(List.of("cert1.pem", "cert2.pem")).keyPaths(List.of("key1.pem")).build();

        assertThatThrownBy(keyStore::keyCertOptions)
            .isInstanceOf(KeyStore.KeyStoreCertOptionsException.class)
            .hasMessageContaining("must have the same size");
    }
}
