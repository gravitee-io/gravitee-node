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
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.X509TrustManager;
import org.assertj.core.api.Assertions;
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
    private final List<TrustStoreLoaderManager> managers = new ArrayList<>();

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

    private TrustStoreLoaderManager managerSendingClientCertificateAuthorities() throws Exception {
        TrustStoreLoaderManager manager = new TrustStoreLoaderManager("fake", platformKeystoreLoader, true);
        // registered before start, so the file watcher threads are released even if an assertion fails
        managers.add(manager);
        manager.start();
        return manager;
    }

    @AfterEach
    void end() {
        cut.stop();
        managers.forEach(TrustStoreLoaderManager::stop);
        managers.clear();
    }

    @Test
    void should_load_platform_key_store() throws Exception {
        cut.start();
        assertThat(cut.getCertificateManager()).isNotNull();
        assertThat(cut.loaders()).containsEntry(platformKeystoreLoader.id(), platformKeystoreLoader);
        assertThat(cut.aliases()).hasSize(2).allMatch(alias -> alias.startsWith(platformKeystoreLoader.id()));
    }

    /**
     * Certificates registered dynamically at runtime (per-subscription mTLS client certificates in APIM) must be
     * trusted, but must never end up in the TLS {@code certificate_authorities} list sent to every client of the
     * listener.
     */
    @Test
    void should_send_no_certificate_authority_by_default() throws Exception {
        cut.start();
        cut.registerLoader(dynamicLoader());

        assertThat(cut.aliases()).hasSize(3);
        assertThat(cut.getCertificateManager().getAcceptedIssuers()).isEmpty();
    }

    @Test
    void should_send_only_the_configured_trust_store_when_sending_authorities() throws Exception {
        TrustStoreLoaderManager manager = managerSendingClientCertificateAuthorities();

        X509Certificate[] platformOnlyIssuers = manager.getCertificateManager().getAcceptedIssuers();
        assertThat(platformOnlyIssuers).hasSize(2);

        manager.registerLoader(dynamicLoader());

        // the dynamic entry did join the trust store...
        assertThat(manager.aliases()).hasSize(3);
        // ...but the advertised issuers are unchanged
        assertThat(manager.getCertificateManager().getAcceptedIssuers())
            .hasSize(2)
            .containsExactlyInAnyOrder(platformOnlyIssuers)
            .doesNotContain(readCertificate("/truststores/client1.crt"));
    }

    @Test
    void should_keep_trusting_dynamically_registered_certificates_it_does_not_send() throws Exception {
        cut.start();
        cut.registerLoader(dynamicLoader());

        X509Certificate client1 = readCertificate("/truststores/client1.crt");
        X509TrustManager trustManager = cut.getCertificateManager();
        // nothing is advertised, yet the certificate must remain a valid trust anchor

        assertThat(trustManager.getAcceptedIssuers()).doesNotContain(client1);
        Assertions
            .assertThatCode(() -> trustManager.checkClientTrusted(new X509Certificate[] { client1 }, client1.getSigAlgName()))
            .doesNotThrowAnyException();
    }

    @Test
    void should_refresh_sent_authorities_when_a_dynamic_loader_is_unloaded() throws Exception {
        TrustStoreLoaderManager manager = managerSendingClientCertificateAuthorities();
        X509Certificate[] platformOnlyIssuers = manager.getCertificateManager().getAcceptedIssuers();
        AbstractKeyStoreLoader dynamic = dynamicLoader();
        manager.registerLoader(dynamic);

        dynamic.onEvent(new KeyStoreEvent.UnloadEvent(dynamic.id()));

        assertThat(manager.aliases()).hasSize(2);
        assertThat(manager.getCertificateManager().getAcceptedIssuers()).containsExactlyInAnyOrder(platformOnlyIssuers);
    }

    @Test
    void should_stop_sending_platform_certificates_removed_from_the_truststore() throws Exception {
        TrustStoreLoaderManager manager = managerSendingClientCertificateAuthorities();
        assertThat(manager.getCertificateManager().getAcceptedIssuers()).hasSize(2);

        // the platform loader reloads (file watch) with a truststore holding a single certificate
        ((AbstractKeyStoreLoader) platformKeystoreLoader).onEvent(
                new KeyStoreEvent.LoadEvent(
                    platformKeystoreLoader.id(),
                    KeyStoreUtils.initFromPath(
                        KeyStoreLoader.CERTIFICATE_FORMAT_JKS,
                        "src/test/resources/truststores/truststore1.jks",
                        "secret"
                    ),
                    "secret"
                )
            );

        assertThat(manager.getCertificateManager().getAcceptedIssuers())
            .hasSize(1)
            .containsExactly(readCertificate("/truststores/client1.crt"));
    }

    private AbstractKeyStoreLoader dynamicLoader() {
        return (AbstractKeyStoreLoader) trustStoreLoaderFactory.create(
            TrustStoreLoaderOptions
                .builder()
                .paths(List.of("src/test/resources/truststores/truststore1.jks"))
                .type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
                .password("secret")
                .build()
        );
    }

    private X509Certificate readCertificate(String path) throws Exception {
        URL resource = this.getClass().getResource(path);
        assertThat(resource).isNotNull();
        try (var is = resource.openStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
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
