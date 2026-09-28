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
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * What happens to a client certificate that is presented on a listener where presenting one is optional
 * ({@code clientAuth: request}, i.e. {@link SSLServerSocket#setWantClientAuth(boolean)}).
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateHandshakeTest {

    private static final String PASSWORD = HandshakeSupport.PASSWORD;

    private static final boolean STRICT = true;
    private static final boolean LENIENT = false;

    /** No trust store was ever pushed to the delegator, the state of a listener with nothing configured. */
    private static final KeyStore NOTHING_LOADED = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void should_accept_any_client_certificate_when_no_trust_store_was_ever_loaded() throws Exception {
        Identity client = valid("client");

        Outcome outcome = handshake(NOTHING_LOADED, STRICT, client);

        assertThat(outcome.failure()).isNull();
        assertThat(outcome.peerCertificates()).isNotEmpty();
    }

    @Test
    void should_accept_any_client_certificate_when_the_last_registered_certificate_is_gone() throws Exception {
        Identity client = valid("client");

        Outcome outcome = handshake(emptyTrustStore(), LENIENT, client);

        assertThat(outcome.failure()).isNull();
        assertThat(outcome.peerCertificates()).isNotEmpty();
    }

    @Test
    void should_accept_an_untrusted_client_certificate_when_presenting_one_is_optional() throws Exception {
        Identity subscription = valid("subscription");
        Identity client = valid("client");

        Outcome outcome = handshake(trustStoreOf(subscription), LENIENT, client);

        assertThat(outcome.failure()).isNull();
        // the certificate still reaches the application, which is what matches it against the subscriptions
        assertThat(outcome.peerCertificates()).isNotEmpty();
    }

    @Test
    void should_reject_a_client_certificate_that_is_not_in_the_trust_store() throws Exception {
        Identity subscription = valid("subscription");
        Identity client = valid("client");

        Outcome outcome = handshake(trustStoreOf(subscription), STRICT, client);

        assertThat(outcome.failure()).isNotNull();
    }

    @Test
    void should_accept_a_client_certificate_that_is_in_the_trust_store() throws Exception {
        Identity subscription = valid("subscription");

        Outcome outcome = handshake(trustStoreOf(subscription), STRICT, subscription);

        assertThat(outcome.failure()).isNull();
        assertThat(outcome.peerCertificates()).isNotEmpty();
    }

    @Test
    void should_reject_an_expired_client_certificate_even_when_presenting_one_is_optional() throws Exception {
        Authority authority = authority("issuer");
        Identity expired = authority.issue("client", Instant.now().minus(400, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS));

        Outcome outcome = handshake(trustStoreOf(authority.certificate()), LENIENT, expired);

        // an unknown certificate is downgraded, a verdict the trust anchors could reach on their own is not
        assertThat(outcome.failure()).isNotNull();
    }

    /** Control for the test above: it proves the rejection there comes from the expiry, not from a broken chain. */
    @Test
    void should_accept_a_valid_client_certificate_issued_by_a_registered_authority_when_presenting_one_is_optional() throws Exception {
        Authority authority = authority("issuer");
        Identity client = authority.issue("client", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS));

        Outcome outcome = handshake(trustStoreOf(authority.certificate()), LENIENT, client);

        assertThat(outcome.failure()).isNull();
    }

    /**
     * Pins a JDK behaviour this fix depends on: a certificate that is a trust anchor of its own -- which is what a
     * registered mTLS subscription certificate is -- is not checked against its validity period. Expiry of those
     * certificates is therefore already not enforced at the TLS layer, with or without this fix.
     */
    @Test
    void should_not_enforce_the_validity_period_of_a_certificate_registered_in_the_trust_store() throws Exception {
        Identity expired = selfSigned(
            "expired-subscription",
            Instant.now().minus(400, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS)
        );

        Outcome outcome = handshake(trustStoreOf(expired), STRICT, expired);

        assertThat(outcome.failure()).isNull();
    }

    /** A client identity, with the chain it presents during the handshake. */
    private record Identity(PrivateKey key, X509Certificate[] chain) {
        X509Certificate certificate() {
            return chain[0];
        }
    }

    /** A certificate authority, the shape an mTLS subscription takes when it registers a whole PKCS#7 chain. */
    private record Authority(PrivateKey key, X509Certificate certificate) {
        Identity issue(String cn, Instant notBefore, Instant notAfter) throws Exception {
            KeyPair keyPair = keyPair();
            X509Certificate leaf = ClientCertificateHandshakeTest.certificate(
                new X500Principal("CN=" + cn),
                keyPair.getPublic(),
                certificate.getSubjectX500Principal(),
                key,
                notBefore,
                notAfter,
                false
            );
            return new Identity(keyPair.getPrivate(), new X509Certificate[] { leaf, certificate });
        }
    }

    private record Outcome(Throwable failure, Certificate[] peerCertificates) {}

    private static Identity valid(String cn) throws Exception {
        return selfSigned(cn, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS));
    }

    private static Identity selfSigned(String cn, Instant notBefore, Instant notAfter) throws Exception {
        KeyPair keyPair = keyPair();
        X500Principal subject = new X500Principal("CN=" + cn);
        X509Certificate certificate = certificate(subject, keyPair.getPublic(), subject, keyPair.getPrivate(), notBefore, notAfter, false);
        return new Identity(keyPair.getPrivate(), new X509Certificate[] { certificate });
    }

    private static Authority authority(String cn) throws Exception {
        KeyPair keyPair = keyPair();
        X500Principal subject = new X500Principal("CN=" + cn);
        X509Certificate certificate = certificate(
            subject,
            keyPair.getPublic(),
            subject,
            keyPair.getPrivate(),
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(365, ChronoUnit.DAYS),
            true
        );
        return new Authority(keyPair.getPrivate(), certificate);
    }

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static X509Certificate certificate(
        X500Principal subject,
        PublicKey subjectKey,
        X500Principal issuer,
        PrivateKey issuerKey,
        Instant notBefore,
        Instant notAfter,
        boolean certificateAuthority
    ) throws Exception {
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
            issuer,
            BigInteger.valueOf(System.nanoTime()),
            Date.from(notBefore),
            Date.from(notAfter),
            subject,
            subjectKey
        );
        if (certificateAuthority) {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature));
        }
        return new JcaX509CertificateConverter().getCertificate(
            builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey))
        );
    }

    private static KeyStore emptyTrustStore() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, PASSWORD.toCharArray());
        return trustStore;
    }

    private static KeyStore trustStoreOf(Identity... identities) throws Exception {
        X509Certificate[] certificates = new X509Certificate[identities.length];
        for (int i = 0; i < identities.length; i++) {
            certificates[i] = identities[i].certificate();
        }
        return trustStoreOf(certificates);
    }

    private static KeyStore trustStoreOf(X509Certificate... certificates) throws Exception {
        KeyStore trustStore = emptyTrustStore();
        for (int i = 0; i < certificates.length; i++) {
            trustStore.setCertificateEntry("cert-" + i, certificates[i]);
        }
        return trustStore;
    }

    /**
     * Runs a real handshake on a listener that requests, but does not require, a client certificate, and reports
     * both what the server saw and why it refused, if it did.
     */
    private Outcome handshake(KeyStore trustStore, boolean failOnUntrustedClientCertificate, Identity client) throws Exception {
        RefreshableX509TrustManagerDelegator trustManager = new RefreshableX509TrustManagerDelegator(
            "test",
            failOnUntrustedClientCertificate
        );
        if (trustStore != NOTHING_LOADED) {
            trustManager.refresh(trustStore, RefreshableX509TrustManagerDelegator.SEND_NOTHING);
        }

        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(HandshakeSupport.serverKeyManagers(), new TrustManager[] { trustManager }, null);

        SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(clientKeyManagers(client), new TrustManager[] { new HandshakeSupport.TrustEverything() }, null);

        try (SSLServerSocket server = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0)) {
            server.setWantClientAuth(true);
            Future<Outcome> accepted = executor.submit(() -> {
                try (SSLSocket socket = (SSLSocket) server.accept()) {
                    // reading forces the handshake to complete, client certificate included
                    socket.getInputStream().read();
                    return new Outcome(null, socket.getSession().getPeerCertificates());
                } catch (Exception e) {
                    return new Outcome(e, null);
                }
            });
            try (SSLSocket socket = (SSLSocket) clientContext.getSocketFactory().createSocket("localhost", server.getLocalPort())) {
                socket.startHandshake();
                socket.getOutputStream().write('x');
                socket.getOutputStream().flush();
            } catch (Exception e) {
                // a rejected certificate surfaces on the client side too, the server side outcome is what we assert
            }
            return accepted.get(30, TimeUnit.SECONDS);
        }
    }

    private static KeyManager[] clientKeyManagers(Identity identity) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, PASSWORD.toCharArray());
        keyStore.setKeyEntry("client", identity.key(), PASSWORD.toCharArray(), identity.chain());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD.toCharArray());
        return keyManagerFactory.getKeyManagers();
    }
}
