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
package io.gravitee.node.certificates.x509;

import static io.gravitee.node.api.certificate.KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.util.KeyStoreUtils;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pins what actually reaches the wire rather than what {@link RefreshableX509TrustManagerDelegator#getAcceptedIssuers()}
 * returns: the {@code certificate_authorities} of the TLS {@code CertificateRequest}. There is no server-side API for
 * that field, but JSSE hands the decoded list to the <em>client</em> key manager, so an instrumented one captures
 * exactly what the server sent.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TrustManagerHandshakeTest {

    private static final String PASSWORD = "secret";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @ParameterizedTest
    @ValueSource(strings = { "TLSv1.2", "TLSv1.3" })
    void should_send_the_trusted_certificates_when_everything_is_advertisable(String protocol) throws Exception {
        Principal[] sent = handshakeAndCaptureSentAuthorities(RefreshableX509TrustManagerDelegator.SEND_EVERYTHING, protocol);

        assertThat(sent).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "TLSv1.2", "TLSv1.3" })
    void should_send_no_certificate_authority_when_nothing_is_advertisable(String protocol) throws Exception {
        Principal[] sent = handshakeAndCaptureSentAuthorities(RefreshableX509TrustManagerDelegator.SEND_NOTHING, protocol);

        assertThat(sent).isEmpty();
    }

    /**
     * Runs a real handshake against a server whose trust manager is the delegator under test, and returns the
     * authorities the server asked for. The client deliberately answers with no certificate, which is the reported
     * scenario: a caller that uses no client certificate still learns what the listener accepts.
     */
    private Principal[] handshakeAndCaptureSentAuthorities(Predicate<String> advertisableAlias, String protocol) throws Exception {
        RefreshableX509TrustManagerDelegator trustManager = new RefreshableX509TrustManagerDelegator("test");
        trustManager.refresh(
            KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, resource("/truststores/truststore2-3.p12"), PASSWORD),
            advertisableAlias
        );

        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(serverKeyManagers(), new TrustManager[] { trustManager }, null);

        CapturingKeyManager clientKeyManager = new CapturingKeyManager();
        SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(new KeyManager[] { clientKeyManager }, new TrustManager[] { new TrustEverything() }, null);

        try (SSLServerSocket server = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0)) {
            server.setEnabledProtocols(new String[] { protocol });
            server.setWantClientAuth(true);
            Future<?> accepted = executor.submit(() -> {
                try (SSLSocket socket = (SSLSocket) server.accept()) {
                    // read one byte so the TLS 1.3 CertificateRequest is processed before we assert
                    socket.getInputStream().read();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            try (SSLSocket client = (SSLSocket) clientContext.getSocketFactory().createSocket("localhost", server.getLocalPort())) {
                client.setEnabledProtocols(new String[] { protocol });
                client.startHandshake();
                client.getOutputStream().write('x');
                client.getOutputStream().flush();
            }
            accepted.get(30, TimeUnit.SECONDS);
        }
        return clientKeyManager.sentAuthorities;
    }

    private static KeyManager[] serverKeyManagers() throws Exception {
        KeyStore keyStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, resource("/keystores/localhost.p12"), PASSWORD);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD.toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    private static String resource(String path) {
        return TrustManagerHandshakeTest.class.getResource(path).getPath();
    }

    /**
     * Records the {@code certificate_authorities} the server sent, then declines to present a certificate.
     */
    private static class CapturingKeyManager extends X509ExtendedKeyManager {

        private volatile Principal[] sentAuthorities;

        @Override
        public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
            this.sentAuthorities = issuers == null ? new Principal[0] : Arrays.copyOf(issuers, issuers.length);
            return null;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
            return chooseClientAlias(keyTypes, issuers, null);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return new X509Certificate[0];
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return null;
        }
    }

    /**
     * The client is not what is under test here, it only has to complete the handshake.
     */
    private static class TrustEverything extends X509ExtendedTrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
