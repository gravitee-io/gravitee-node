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
package io.gravitee.node.vertx.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import io.gravitee.node.certificates.DefaultCRLLoaderFactoryRegistry;
import io.gravitee.node.certificates.DefaultKeyStoreLoaderFactoryRegistry;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.certificates.crl.FileCRLLoaderFactory;
import io.gravitee.node.certificates.file.FileTrustStoreLoaderFactory;
import io.gravitee.node.certificates.selfsigned.SelfSignedKeyStoreLoaderFactory;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.gravitee.node.vertx.server.tcp.VertxTcpServerOptions;
import io.vertx.rxjava3.core.Vertx;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxServerFactoryTest {

    String ID = "foo";

    private static final TrustStoreLoaderOptions NO_TRUST_STORE = TrustStoreLoaderOptions.builder().build();

    private static final TrustStoreLoaderOptions CONFIGURED_TRUST_STORE = TrustStoreLoaderOptions.builder()
        .paths(List.of("src/test/resources/ssl/truststore.p12"))
        .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
        .password("gravitee")
        // no file watcher: nothing in these tests depends on reloading
        .watch(false)
        .build();

    @Mock
    Vertx vertx;

    private final List<TrustStoreLoaderManager> managers = new ArrayList<>();

    @AfterEach
    void releaseManagers() {
        managers.forEach(TrustStoreLoaderManager::stop);
        managers.clear();
    }

    private VertxServerFactory<?, VertxServerOptions> cut;

    @BeforeEach
    void init() {
        DefaultKeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry =
            new DefaultKeyStoreLoaderFactoryRegistry<>();
        keyStoreLoaderFactoryRegistry.registerFactory(new SelfSignedKeyStoreLoaderFactory());
        DefaultKeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry =
            new DefaultKeyStoreLoaderFactoryRegistry<>();
        trustStoreLoaderFactoryRegistry.registerFactory(new FileTrustStoreLoaderFactory());
        DefaultCRLLoaderFactoryRegistry crlLoaderFactoryRegistry = new DefaultCRLLoaderFactoryRegistry();
        crlLoaderFactoryRegistry.registerFactory(new FileCRLLoaderFactory());
        cut = new VertxServerFactory<>(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry);
    }

    @Test
    void should_create_vertx_unsecured_http_server() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder()
            .prefix(ID)
            .environment(new MockEnvironment())
            // forcing ID
            .id(ID)
            .keyStoreLoaderOptions(KeyStoreLoaderOptions.builder().build())
            .trustStoreLoaderOptions(TrustStoreLoaderOptions.builder().build())
            .build();
        final VertxServer<?, VertxServerOptions> vertxServer = cut.create(options);

        assertThat(vertxServer).isNotNull();
        assertThat(vertxServer.id()).isEqualTo(ID);
        assertThat(vertxServer.options()).isEqualTo(options);
        assertThat(vertxServer.instances()).isEmpty();
    }

    @Test
    void should_create_vertx_net_server() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder()
            .prefix(ID)
            .environment(new MockEnvironment())
            // forcing ID
            .id(ID)
            .secured(true)
            .sni(true)
            .keyStoreLoaderOptions(KeyStoreLoaderOptions.builder().build())
            .trustStoreLoaderOptions(TrustStoreLoaderOptions.builder().build())
            .build();
        final VertxServer<?, VertxServerOptions> vertxServer = cut.create(options);

        assertThat(vertxServer).isNotNull();
        assertThat(vertxServer.id()).isEqualTo(ID);
        assertThat(vertxServer.options()).isEqualTo(options);
        assertThat(vertxServer.instances()).isEmpty();
    }

    @Test
    void should_not_send_client_certificate_authorities_by_default() throws Exception {
        VertxServer<?, VertxServerOptions> vertxServer = cut.create(securedOptions(false));

        TrustStoreLoaderManager trustStoreLoaderManager = vertxServer.trustStoreLoaderManager();
        trustStoreLoaderManager.start();
        try {
            assertThat(trustStoreLoaderManager.getCertificateManager().getAcceptedIssuers()).isEmpty();
        } finally {
            trustStoreLoaderManager.stop();
        }
    }

    @Test
    void should_send_the_configured_trust_store_when_the_option_is_set() throws Exception {
        VertxServer<?, VertxServerOptions> vertxServer = cut.create(securedOptions(true));

        TrustStoreLoaderManager trustStoreLoaderManager = vertxServer.trustStoreLoaderManager();
        trustStoreLoaderManager.start();
        try {
            assertThat(trustStoreLoaderManager.getCertificateManager().getAcceptedIssuers()).hasSize(2);
        } finally {
            trustStoreLoaderManager.stop();
        }
    }

    @Test
    void should_accept_an_untrusted_client_certificate_when_a_certificate_is_only_requested_and_no_trust_store_is_configured()
        throws Exception {
        X509TrustManager trustManager = trustManagerWithARuntimeRegisteredCertificate(listener("request", NO_TRUST_STORE));

        assertThatNoException().isThrownBy(() -> trustManager.checkClientTrusted(untrustedChain(), "RSA"));
    }

    @Test
    void should_reject_an_untrusted_client_certificate_when_a_certificate_is_required() throws Exception {
        X509TrustManager trustManager = trustManagerWithARuntimeRegisteredCertificate(listener("required", NO_TRUST_STORE));

        assertThatExceptionOfType(CertificateException.class).isThrownBy(() -> trustManager.checkClientTrusted(untrustedChain(), "RSA"));
    }

    @Test
    void should_reject_an_untrusted_client_certificate_when_no_certificate_is_requested_at_all() throws Exception {
        X509TrustManager trustManager = trustManagerWithARuntimeRegisteredCertificate(listener("none", NO_TRUST_STORE));

        assertThatExceptionOfType(CertificateException.class).isThrownBy(() -> trustManager.checkClientTrusted(untrustedChain(), "RSA"));
    }

    @Test
    void should_reject_an_untrusted_client_certificate_when_a_trust_store_is_configured() throws Exception {
        X509TrustManager trustManager = trustManagerWithARuntimeRegisteredCertificate(listener("request", CONFIGURED_TRUST_STORE));

        assertThatExceptionOfType(CertificateException.class).isThrownBy(() -> trustManager.checkClientTrusted(untrustedChain(), "RSA"));
    }

    /**
     * Brings the listener to the state that reveals the bug: a certificate registered at runtime, as an mTLS plan
     * subscription does, which is the only thing that ever fills the trust store of a listener without a configured
     * one.
     */
    private X509TrustManager trustManagerWithARuntimeRegisteredCertificate(VertxHttpServerOptions options) throws Exception {
        TrustStoreLoaderManager trustStoreLoaderManager = cut.create(options).trustStoreLoaderManager();
        managers.add(trustStoreLoaderManager);
        trustStoreLoaderManager.start();
        trustStoreLoaderManager.registerLoader(new InMemoryTrustStoreLoader(selfSigned("subscription")));
        return trustStoreLoaderManager.getCertificateManager();
    }

    private static X509Certificate[] untrustedChain() throws Exception {
        return new X509Certificate[] { selfSigned("unknown-client") };
    }

    private static X509Certificate selfSigned(String cn) throws Exception {
        KeyStore keyStore = KeyStoreUtils.initSelfSigned(cn, "secret");
        String alias = Collections.list(keyStore.aliases()).get(0);
        return (X509Certificate) keyStore.getCertificateChain(alias)[0];
    }

    private VertxHttpServerOptions listener(String clientAuth, TrustStoreLoaderOptions trustStoreLoaderOptions) {
        return VertxHttpServerOptions.builder()
            .prefix(ID)
            .environment(new MockEnvironment())
            .id(ID)
            .secured(true)
            .clientAuth(clientAuth.toUpperCase())
            .keyStoreLoaderOptions(KeyStoreLoaderOptions.builder().build())
            .trustStoreLoaderOptions(trustStoreLoaderOptions)
            .build();
    }

    /** Stands for the per-subscription certificates APIM registers on a running listener. */
    private static class InMemoryTrustStoreLoader extends AbstractKeyStoreLoader<TrustStoreLoaderOptions> {

        private final X509Certificate certificate;

        InMemoryTrustStoreLoader(X509Certificate certificate) {
            super(TrustStoreLoaderOptions.builder().build());
            this.certificate = certificate;
        }

        @Override
        public void start() {
            try {
                KeyStore keyStore = KeyStore.getInstance(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12);
                keyStore.load(null, getPassword().toCharArray());
                keyStore.setCertificateEntry("cert", certificate);
                onEvent(new KeyStoreEvent.LoadEvent(id(), keyStore, getPassword()));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void stop() {
            // nothing to stop
        }
    }

    private VertxHttpServerOptions securedOptions(boolean sendClientCertificateAuthorities) {
        return VertxHttpServerOptions.builder()
            .prefix(ID)
            .environment(new MockEnvironment())
            .id(ID)
            .secured(true)
            .sendClientCertificateAuthorities(sendClientCertificateAuthorities)
            .keyStoreLoaderOptions(KeyStoreLoaderOptions.builder().build())
            .trustStoreLoaderOptions(
                TrustStoreLoaderOptions.builder()
                    .paths(List.of("src/test/resources/ssl/truststore.p12"))
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                    .password("gravitee")
                    .build()
            )
            .build();
    }

    @Test
    void should_throw_illegal_argument_exception_when_create_unsupported_vertx_server() {
        final VertxServerOptions options = mock(VertxServerOptions.class);
        final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> cut.create(options));

        assertThat(illegalArgumentException.getMessage()).isEqualTo(
            "Server type is not a supported vertx server (option class=[VertxServerOptions])"
        );
    }
}
