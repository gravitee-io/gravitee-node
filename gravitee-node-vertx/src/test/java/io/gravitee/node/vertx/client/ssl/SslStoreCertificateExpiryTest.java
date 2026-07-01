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
package io.gravitee.node.vertx.client.ssl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.node.vertx.client.ssl.jks.JKSTrustStore;
import io.gravitee.node.vertx.client.ssl.none.NoneTrustStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMKeyStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMTrustStore;
import io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12KeyStore;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

class SslStoreCertificateExpiryTest {

    private static final String PASSWORD = "secret";

    private final Logger log = org.mockito.Mockito.mock(Logger.class);
    private KeyPair keyPair;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @Test
    void should_warn_for_expiring_pem_truststore_content() throws Exception {
        var trustStore = new PEMTrustStore(null, pem(expiringCertificate()));

        trustStore.warnIfCertificateExpired(log);

        verify(log).warn(contains("expires in"), contains("client truststore (PEM)"), anyLong(), any());
    }

    @Test
    void should_warn_for_expiring_pem_keystore_content() throws Exception {
        var keyStore = PEMKeyStore.builder().certContent(pem(expiringCertificate())).keyContent("ignored").build();

        keyStore.warnIfCertificateExpired(log);

        verify(log).warn(contains("expires in"), contains("client keystore (PEM)"), anyLong(), any());
    }

    @Test
    void should_warn_for_expiring_jks_truststore_path() throws Exception {
        Path jks = writeStore("truststore.jks", "JKS", trustedStore("JKS", expiringCertificate()));
        var trustStore = new JKSTrustStore(jks.toString(), null, PASSWORD, null);

        trustStore.warnIfCertificateExpired(log);

        verify(log).warn(contains("expires in"), eq("ca"), anyLong(), any());
    }

    @Test
    void should_warn_for_expiring_pkcs12_keystore_content() throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry(
            "key",
            keyPair.getPrivate(),
            PASSWORD.toCharArray(),
            new java.security.cert.Certificate[] { expiringCertificate() }
        );
        String content = Base64.getEncoder().encodeToString(toBytes(store));
        var keyStore = new PKCS12KeyStore(null, content, PASSWORD, null, PASSWORD);

        keyStore.warnIfCertificateExpired(log);

        verify(log).warn(contains("expires in"), eq("key"), anyLong(), any());
    }

    @Test
    void should_not_warn_for_healthy_certificate() throws Exception {
        var trustStore = new PEMTrustStore(
            null,
            pem(certificate(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS)))
        );

        trustStore.warnIfCertificateExpired(log);

        verify(log, never()).warn(any(String.class), any(), any(), any());
        verify(log, never()).error(any(String.class), any(), any());
    }

    @Test
    void should_do_nothing_for_none_truststore() {
        new NoneTrustStore().warnIfCertificateExpired(log);

        verifyNoInteractions(log);
    }

    @Test
    void should_swallow_errors_and_never_throw() {
        // Non-existent path and unreadable content must not raise.
        new JKSTrustStore("/does/not/exist.jks", null, PASSWORD, null).warnIfCertificateExpired(log);
        new PEMTrustStore("/does/not/exist.pem", null).warnIfCertificateExpired(log);
    }

    // The Cockpit / Exchange WebSocket connector (and every other consumer) reaches the expiry check
    // only because trustOptions() / keyCertOptions() invoke it — not through the node client factories.

    @Test
    void trust_options_should_trigger_expiry_check() throws Exception {
        Path jks = writeStore("truststore.jks", "JKS", trustedStore("JKS", expiringCertificate()));
        var trustStore = spy(new JKSTrustStore(jks.toString(), null, PASSWORD, null));

        trustStore.trustOptions();

        verify(trustStore).warnIfCertificateExpired(any());
    }

    @Test
    void key_cert_options_should_trigger_expiry_check() throws Exception {
        var keyStore = spy(PEMKeyStore.builder().certContent(pem(expiringCertificate())).keyContent("ignored").build());

        keyStore.keyCertOptions();

        verify(keyStore).warnIfCertificateExpired(any());
    }

    private X509Certificate expiringCertificate() throws Exception {
        return certificate(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS));
    }

    private X509Certificate certificate(Instant notBefore, Instant notAfter) throws Exception {
        X500Principal subject = new X500Principal("CN=test");
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        var holder = new JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(System.nanoTime()),
            Date.from(notBefore),
            Date.from(notAfter),
            subject,
            keyPair.getPublic()
        )
            .build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private KeyStore trustedStore(String type, X509Certificate certificate) throws Exception {
        KeyStore store = KeyStore.getInstance(type);
        store.load(null, null);
        store.setCertificateEntry("ca", certificate);
        return store;
    }

    private Path writeStore(String name, String type, KeyStore store) throws Exception {
        Path path = tempDir.resolve(name);
        Files.write(path, toBytes(store));
        return path;
    }

    private byte[] toBytes(KeyStore store) throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        store.store(out, PASSWORD.toCharArray());
        return out.toByteArray();
    }

    private String pem(X509Certificate certificate) throws Exception {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(certificate);
        }
        return writer.toString();
    }
}
