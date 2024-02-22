/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.node.certificates;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.certificates.file.FileTrustStoreLoaderFactory;
import java.security.KeyStore;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TrustStoreLoaderManagerTest {

    TrustStoreLoaderManager cut;
    private KeyStoreLoader platformKeystoreLoader;
    private final FileTrustStoreLoaderFactory trustStoreLoaderFactory = new FileTrustStoreLoaderFactory();

    @BeforeEach
    void begin() {
        platformKeystoreLoader =
            trustStoreLoaderFactory.create(
                TrustStoreLoaderOptions
                    .builder()
                    .paths(List.of("src/test/resources/truststores/truststore2-3.p12"))
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                    .password("secret")
                    .build()
            );
        cut = new TrustStoreLoaderManager("fake", platformKeystoreLoader);
    }

    @AfterEach
    void end() {
        cut.stop();
    }

    @Test
    void should_load_platform_key_store() throws Exception {
        cut.start();
        assertThat(cut.getCertificateManager()).isNotNull();
        assertThat(cut.loaders()).containsEntry(platformKeystoreLoader.id(), platformKeystoreLoader);
        assertThat(cut.aliases()).hasSize(2).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));
    }

    @Test
    void should_add_and_remove_keystore_to_main() throws Exception {
        cut.start();

        AbstractKeyStoreLoader keyStoreLoader = (AbstractKeyStoreLoader) trustStoreLoaderFactory.create(
            TrustStoreLoaderOptions
                .builder()
                .paths(List.of("src/test/resources/truststores/truststore1.jks"))
                .type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
                .password("secret")
                .build()
        );

        cut.registerLoader(keyStoreLoader);
        assertThat(cut.aliases()).hasSize(3).anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));

        String loaderId = keyStoreLoader.id();
        keyStoreLoader.onEvent(new KeyStoreEvent.UnloadEvent(loaderId));

        assertThat(cut.aliases()).hasSize(2);
    }

    @Test
    void should_add_remove_p12_keystore_to_main_jks() throws Exception {
        platformKeystoreLoader =
            trustStoreLoaderFactory.create(
                TrustStoreLoaderOptions
                    .builder()
                    .paths(List.of("src/test/resources/truststores/truststore1.jks"))
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
                    .password("secret")
                    .build()
            );
        cut = new TrustStoreLoaderManager("fake", platformKeystoreLoader);
        cut.start();

        assertThat(cut.aliases()).hasSize(1).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));

        AbstractKeyStoreLoader keyStoreLoader = (AbstractKeyStoreLoader) trustStoreLoaderFactory.create(
            TrustStoreLoaderOptions
                .builder()
                .paths(List.of("src/test/resources/truststores/truststore2-3.p12"))
                .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                .password("secret")
                .build()
        );
        cut.registerLoader(keyStoreLoader);
        assertThat(cut.aliases()).hasSize(3).anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));

        String loaderId = keyStoreLoader.id();
        keyStoreLoader.onEvent(new KeyStoreEvent.UnloadEvent(loaderId));

        assertThat(cut.aliases()).hasSize(1);
    }

    @Test
    void should_update_platform_truststore() throws Exception {
        cut.start();
        assertThat(cut.aliases()).hasSize(2).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));

        AbstractKeyStoreLoader keyStoreLoader = (AbstractKeyStoreLoader) trustStoreLoaderFactory.create(
            TrustStoreLoaderOptions
                .builder()
                .paths(List.of("src/test/resources/truststores/truststore1.jks"))
                .type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
                .password("secret")
                .build()
        );
        cut.registerLoader(keyStoreLoader);
        assertThat(cut.aliases()).hasSize(3);

        KeyStore keyStore = KeyStoreUtils.initFromPath(KeyStoreUtils.TYPE_JKS, "src/test/resources/truststores/truststore1.jks", "secret");
        String loaderId = platformKeystoreLoader.id();
        ((AbstractKeyStoreLoader) platformKeystoreLoader).onEvent(new KeyStoreEvent.LoadEvent(loaderId, keyStore, "secret"));
        assertThat(cut.aliases())
            .hasSize(2)
            .anyMatch(alias -> alias.startsWith(platformKeystoreLoader.id()))
            .anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));
    }

    @Test
    void should_add_private_ca_in_platform_truststore() throws Exception {
        platformKeystoreLoader =
            trustStoreLoaderFactory.create(
                TrustStoreLoaderOptions
                    .builder()
                    .paths(List.of("src/test/resources/keystores/ca.p12"))
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                    .password("ca-secret")
                    .build()
            );
        cut = new TrustStoreLoaderManager("fake", platformKeystoreLoader);
        cut.start();

        assertThat(cut.aliases()).hasSize(1).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));
    }
}
