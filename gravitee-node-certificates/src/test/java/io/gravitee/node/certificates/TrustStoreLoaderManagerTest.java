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

import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.certificates.file.FileTrustStoreLoaderFactory;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TrustStoreLoaderManagerTest {

    TrustStoreLoaderManager underTest;
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
        underTest = new TrustStoreLoaderManager("fake", platformKeystoreLoader);
    }

    @AfterEach
    void end() {
        underTest.stop();
    }

    @Test
    void should_load_platform_key_store() throws Exception {
        underTest.start();
        assertThat(underTest.getCertificateManager()).isNotNull();
        assertThat(underTest.loaders()).containsEntry(platformKeystoreLoader.id(), platformKeystoreLoader);
        assertThat(underTest.aliases()).hasSize(2).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));
    }

    @Test
    void should_add_and_remove_keystore_to_main() throws Exception {
        underTest.start();

        AbstractKeyStoreLoader keyStoreLoader = (AbstractKeyStoreLoader) trustStoreLoaderFactory.create(
            TrustStoreLoaderOptions
                .builder()
                .paths(List.of("src/test/resources/truststores/truststore1.jks"))
                .type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
                .password("secret")
                .build()
        );

        underTest.registerLoader(keyStoreLoader);
        assertThat(underTest.aliases()).hasSize(3).anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));

        keyStoreLoader.onEvent(new KeyStoreEvent(KeyStoreEvent.EventType.UNLOAD, keyStoreLoader.id(), null, null, null));

        assertThat(underTest.aliases()).hasSize(2);
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
        underTest = new TrustStoreLoaderManager("fake", platformKeystoreLoader);
        underTest.start();

        assertThat(underTest.aliases()).hasSize(1).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));

        AbstractKeyStoreLoader keyStoreLoader = (AbstractKeyStoreLoader) trustStoreLoaderFactory.create(
            TrustStoreLoaderOptions
                .builder()
                .paths(List.of("src/test/resources/truststores/truststore2-3.p12"))
                .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                .password("secret")
                .build()
        );
        underTest.registerLoader(keyStoreLoader);
        assertThat(underTest.aliases()).hasSize(3).anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));

        keyStoreLoader.onEvent(new KeyStoreEvent(KeyStoreEvent.EventType.UNLOAD, keyStoreLoader.id(), null, null, null));

        assertThat(underTest.aliases()).hasSize(1);
    }
}
