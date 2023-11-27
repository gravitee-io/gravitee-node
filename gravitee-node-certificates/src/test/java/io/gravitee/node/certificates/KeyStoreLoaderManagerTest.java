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
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.file.FileKeyStoreLoaderFactory;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KeyStoreLoaderManagerTest {

    KeyStoreLoaderManager underTest;
    private KeyStoreLoader platformKeystoreLoader;
    private FileKeyStoreLoaderFactory keyStoreLoaderFactory;

    @BeforeEach
    void begin() {
        keyStoreLoaderFactory = new FileKeyStoreLoaderFactory();
        platformKeystoreLoader =
            keyStoreLoaderFactory.create(
                KeyStoreLoaderOptions
                    .builder()
                    .paths(List.of("src/test/resources/keystores/all-in-one.p12"))
                    .password("secret")
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                    .build()
            );
        underTest = new KeyStoreLoaderManager("fake", platformKeystoreLoader, true);
    }

    @AfterEach
    void end() {
        underTest.stop();
    }

    @Test
    void should_load_platform_key_store() throws Exception {
        underTest.start();
        assertThat(underTest.getKeyManager()).isNotNull();
        assertThat(underTest.loaders()).containsEntry(platformKeystoreLoader.id(), platformKeystoreLoader);
        assertThat(underTest.aliases()).hasSize(4).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));
    }

    @Test
    void should_add_and_remove_keystore_to_main() throws Exception {
        underTest.start();

        AbstractKeyStoreLoader keyStoreLoader = (AbstractKeyStoreLoader) keyStoreLoaderFactory.create(
            KeyStoreLoaderOptions
                .builder()
                .paths(List.of("src/test/resources/keystores/wildcard.jks"))
                .password("secret")
                .watch(false)
                .type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
                .build()
        );

        underTest.registerLoader(keyStoreLoader);
        assertThat(underTest.aliases()).hasSize(5).anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));

        keyStoreLoader.onEvent(new KeyStoreEvent(KeyStoreEvent.EventType.UNLOAD, keyStoreLoader.id(), null, null, null));

        assertThat(underTest.aliases()).hasSize(4);
    }
}
