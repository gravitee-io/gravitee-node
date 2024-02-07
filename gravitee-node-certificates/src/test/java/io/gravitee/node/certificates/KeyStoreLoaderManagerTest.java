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
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.file.FileKeyStoreLoaderFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import javax.net.ssl.X509ExtendedKeyManager;
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
        underTest = new KeyStoreLoaderManager("fake", platformKeystoreLoader, true, null);
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

        String loaderId = keyStoreLoader.id();
        keyStoreLoader.onEvent(new KeyStoreEvent.UnloadEvent(loaderId));

        assertThat(underTest.aliases()).hasSize(4);
    }

    @Test
    void should_update_platform_keystore() throws Exception {
        underTest.start();
        assertThat(underTest.aliases()).hasSize(4).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));

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
        assertThat(underTest.aliases()).hasSize(5);

        KeyStore keyStore = KeyStoreUtils.initFromPath(
            KeyStoreUtils.DEFAULT_KEYSTORE_TYPE,
            "src/test/resources/keystores/wildcard.p12",
            "secret"
        );
        String loaderId = platformKeystoreLoader.id();
        ((AbstractKeyStoreLoader) platformKeystoreLoader).onEvent(new KeyStoreEvent.LoadEvent(loaderId, keyStore, "secret"));
        assertThat(underTest.aliases())
            .hasSize(2)
            .anyMatch(alias -> alias.startsWith(platformKeystoreLoader.id()))
            .anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));
    }

    @Test
    void should_update_platform_keystore_with_mix_of_private_keys_and_trusted_entries() throws Exception {
        underTest.start();
        assertThat(underTest.aliases()).hasSize(4).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));

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
        assertThat(underTest.aliases()).hasSize(5);

        KeyStore keyStore = KeyStoreUtils.initFromPath(
            KeyStoreUtils.DEFAULT_KEYSTORE_TYPE,
            "src/test/resources/keystores/mixed-entries.p12",
            "secret"
        );
        String loaderId = platformKeystoreLoader.id();
        ((AbstractKeyStoreLoader) platformKeystoreLoader).onEvent(new KeyStoreEvent.LoadEvent(loaderId, keyStore, "secret"));
        assertThat(underTest.aliases())
            .hasSize(7)
            .anyMatch(alias -> alias.startsWith(platformKeystoreLoader.id()))
            .anyMatch(alias -> alias.startsWith(keyStoreLoader.id()));
    }

    @Test
    void should_fallback_to_default_alias_when_no_sni_and_find_entry_for_it()
        throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        platformKeystoreLoader =
            keyStoreLoaderFactory.create(
                KeyStoreLoaderOptions
                    .builder()
                    .paths(List.of("src/test/resources/keystores/all-in-one.p12"))
                    .password("secret")
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                    .build()
            );
        underTest = new KeyStoreLoaderManager("fake", platformKeystoreLoader, false, "localhost2");
        underTest.start();
        assertThat(underTest.aliases()).hasSize(4);

        String alias = ((X509ExtendedKeyManager) underTest.getKeyManager()).chooseEngineServerAlias("void", null, null);
        assertThat(alias).contains("localhost2"); // the actual alias in the keystore is different, but we check that it is based on the original alias name
        assertThat(underTest.getKeyManager().getPrivateKey(alias)).isNotNull();
    }
}
