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

package io.gravitee.node.certificates.x509;

import static io.gravitee.node.api.certificate.KeyStoreLoader.CERTIFICATE_FORMAT_JKS;
import static io.gravitee.node.api.certificate.KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.common.util.KeyStoreUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@RunWith(MockitoJUnitRunner.class)
class RefreshableX509TrustManagerDelegatorTest {

    public static final String PASSWORD = "secret";

    @Mock
    private SSLEngine sslEngine;

    @Mock
    private Socket socket;

    private RefreshableX509TrustManagerDelegator cut;

    @BeforeEach
    public void before() {
        cut = new RefreshableX509TrustManagerDelegator("http");
    }

    @Test
    void should_not_fail_on_empty_truststore() {
        X509Certificate[] emptyChain = new X509Certificate[] {};
        assertThatCode(() -> cut.checkServerTrusted(emptyChain, null, (Socket) null)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(emptyChain, null, (SSLEngine) null)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(emptyChain, null)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(emptyChain, null, (Socket) null)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(emptyChain, null, (SSLEngine) null)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(emptyChain, null)).doesNotThrowAnyException();
        assertThat(cut.getAcceptedIssuers()).isNotNull();
        assertThat(cut.getAcceptedIssuers()).isEmpty();
    }

    @Test
    void should_load_truststore_store_and_trust_certs() throws CertificateException, IOException {
        KeyStore trustStore = loadTruststore();
        cut.refresh(trustStore);
        assertThat(cut.getAcceptedIssuers()).hasSize(2);
        URL resource = this.getClass().getResource("/truststores/client2.crt");
        assertThat(resource).isNotNull();
        try (var is = resource.openStream()) {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            assertThat(cut.getAcceptedIssuers()).contains(certificate);
            X509Certificate[] chain = new X509Certificate[] { certificate };
            assertThatCode(() -> cut.checkClientTrusted(chain, certificate.getSigAlgName())).doesNotThrowAnyException();
            assertThatCode(() -> cut.checkClientTrusted(chain, certificate.getSigAlgName(), sslEngine)).doesNotThrowAnyException();
            assertThatCode(() -> cut.checkClientTrusted(chain, certificate.getSigAlgName(), socket)).doesNotThrowAnyException();
            assertThatCode(() -> cut.checkServerTrusted(chain, certificate.getSigAlgName())).doesNotThrowAnyException();
            assertThatCode(() -> cut.checkServerTrusted(chain, certificate.getSigAlgName(), sslEngine)).doesNotThrowAnyException();
            assertThatCode(() -> cut.checkServerTrusted(chain, certificate.getSigAlgName(), socket)).doesNotThrowAnyException();
        }
    }

    @Test
    void should_load_truststore_store_and_do_not_trust_certs() throws CertificateException, IOException {
        KeyStore trustStore = loadTruststore();
        cut.refresh(trustStore);
        assertThat(cut.getAcceptedIssuers()).hasSize(2);
        URL resource = this.getClass().getResource("/truststores/client1.crt");
        assertThat(resource).isNotNull();
        try (var is = resource.openStream()) {
            X509Certificate untrusted = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            assertThat(cut.getAcceptedIssuers()).doesNotContain(untrusted);
            X509Certificate[] chain = new X509Certificate[] { untrusted };
            assertThatCode(() -> cut.checkClientTrusted(chain, untrusted.getSigAlgName()))
                .hasMessageContaining("PKIX path validation failed");
            assertThatCode(() -> cut.checkClientTrusted(chain, untrusted.getSigAlgName(), sslEngine))
                .hasMessageContaining("PKIX path validation failed");
            assertThatCode(() -> cut.checkClientTrusted(chain, untrusted.getSigAlgName(), socket))
                .hasMessageContaining("PKIX path validation failed");
            assertThatCode(() -> cut.checkServerTrusted(chain, untrusted.getSigAlgName()))
                .hasMessageContaining("PKIX path validation failed");
            assertThatCode(() -> cut.checkServerTrusted(chain, untrusted.getSigAlgName(), sslEngine))
                .hasMessageContaining("PKIX path validation failed");
            assertThatCode(() -> cut.checkServerTrusted(chain, untrusted.getSigAlgName(), socket))
                .hasMessageContaining("PKIX path validation failed");
        }
    }

    @Test
    void should_not_advertise_aliases_rejected_by_the_predicate() throws Exception {
        KeyStore trustStore = loadTruststore();

        cut.refresh(trustStore, "client2"::equals);

        assertThat(cut.getAcceptedIssuers()).containsExactly(readCertificate("/truststores/client2.crt"));
        assertThat(cut.getAcceptedIssuers()).doesNotContain(readCertificate("/truststores/client3.crt"));
    }

    @Test
    void should_still_trust_certificates_it_does_not_advertise() throws Exception {
        KeyStore trustStore = loadTruststore();

        // client3 is kept out of the advertised acceptable issuers...
        cut.refresh(trustStore, "client2"::equals);

        // ...yet it remains a valid trust anchor, which is the whole point: plan matching must keep working.
        X509Certificate client3 = readCertificate("/truststores/client3.crt");
        X509Certificate[] chain = new X509Certificate[] { client3 };
        assertThat(cut.getAcceptedIssuers()).doesNotContain(client3);
        assertThatCode(() -> cut.checkClientTrusted(chain, client3.getSigAlgName())).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, client3.getSigAlgName(), sslEngine)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, client3.getSigAlgName(), socket)).doesNotThrowAnyException();
    }

    @Test
    void should_advertise_nothing_when_no_alias_is_advertisable() {
        cut.refresh(loadTruststore(), alias -> false);

        assertThat(cut.getAcceptedIssuers()).isEmpty();
    }

    @Test
    void should_advertise_every_alias_when_refreshed_without_predicate() {
        cut.refresh(loadTruststore(), new char[0]);

        assertThat(cut.getAcceptedIssuers()).hasSize(2);
    }

    @Test
    void should_not_expose_its_internal_accepted_issuers_array() {
        cut.refresh(loadTruststore());

        X509Certificate[] issuers = cut.getAcceptedIssuers();
        issuers[0] = null;

        assertThat(cut.getAcceptedIssuers()).hasSize(2).doesNotContainNull();
    }

    @Test
    void should_advertise_the_leaf_of_key_entries_like_the_jdk_does() throws Exception {
        // a keystore file reused as a trust store may hold key entries; the JDK trust manager treats the first
        // certificate of their chain as a trust anchor, so we must advertise it too
        KeyStore withKeyEntry = KeyStoreUtils.initFromPath(
            CERTIFICATE_FORMAT_PKCS12,
            this.getClass().getResource("/keystores/all-in-one.p12").getPath(),
            PASSWORD
        );
        for (String alias : Collections.list(withKeyEntry.aliases())) {
            assertThat(withKeyEntry.isKeyEntry(alias)).isTrue();
        }

        cut.refresh(withKeyEntry);

        assertThat(cut.getAcceptedIssuers()).hasSize(4);
    }

    @Test
    void should_not_advertise_the_same_certificate_twice() throws Exception {
        KeyStore duplicated = KeyStore.getInstance(CERTIFICATE_FORMAT_PKCS12);
        duplicated.load(null, PASSWORD.toCharArray());
        X509Certificate certificate = readCertificate("/truststores/client1.crt");
        duplicated.setCertificateEntry("first", certificate);
        duplicated.setCertificateEntry("second", certificate);

        cut.refresh(duplicated);

        assertThat(cut.getAcceptedIssuers()).containsExactly(certificate);
    }

    private X509Certificate readCertificate(String path) throws Exception {
        URL resource = this.getClass().getResource(path);
        assertThat(resource).isNotNull();
        try (var is = resource.openStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
    }

    private KeyStore loadTruststore() {
        URL resource = this.getClass().getResource("/truststores/truststore2-3.p12");
        assertThat(resource).isNotNull();
        return KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, resource.getPath(), PASSWORD);
    }

    @Test
    void should_reject_revoked_certificate_with_crl() throws Exception {
        // Load truststore containing CA certificate
        URL truststoreResource = this.getClass().getResource("/crls/ca-truststore.jks");
        assertThat(truststoreResource).isNotNull();
        KeyStore trustStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_JKS, truststoreResource.getPath(), PASSWORD);
        cut.refresh(trustStore);

        // Load CRL containing revoked certificate
        URL crlResource = this.getClass().getResource("/crls/crl-with-revocations.pem");
        assertThat(crlResource).isNotNull();
        CRL crl;
        try (InputStream is = crlResource.openStream()) {
            crl = CertificateFactory.getInstance("X.509").generateCRL(is);
        }
        cut.refresh(List.of(crl));

        // Load the revoked certificate
        URL certResource = this.getClass().getResource("/crls/cert-client-revoked.pem");
        assertThat(certResource).isNotNull();
        X509Certificate revokedCert;
        try (InputStream is = certResource.openStream()) {
            revokedCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        // Load CA certificate to build complete chain
        URL caResource = this.getClass().getResource("/crls/ca.pem");
        assertThat(caResource).isNotNull();
        X509Certificate caCert;
        try (InputStream is = caResource.openStream()) {
            caCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        X509Certificate[] chain = new X509Certificate[] { revokedCert, caCert };

        // Verify that the revoked certificate is rejected
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA")).isInstanceOf(CertificateException.class).hasMessageContaining("revoked");
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", sslEngine))
            .isInstanceOf(CertificateException.class)
            .hasMessageContaining("revoked");
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", socket))
            .isInstanceOf(CertificateException.class)
            .hasMessageContaining("revoked");
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA")).isInstanceOf(CertificateException.class).hasMessageContaining("revoked");
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", sslEngine))
            .isInstanceOf(CertificateException.class)
            .hasMessageContaining("revoked");
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", socket))
            .isInstanceOf(CertificateException.class)
            .hasMessageContaining("revoked");
    }

    @Test
    void should_accept_non_revoked_certificate_with_crl() throws Exception {
        // Load truststore containing CA certificate
        URL truststoreResource = this.getClass().getResource("/crls/ca-truststore.jks");
        assertThat(truststoreResource).isNotNull();
        KeyStore trustStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_JKS, truststoreResource.getPath(), PASSWORD);
        cut.refresh(trustStore);

        // Load CRL (empty, no revoked certificates)
        URL crlResource = this.getClass().getResource("/crls/crl-empty.pem");
        assertThat(crlResource).isNotNull();
        CRL crl;
        try (InputStream is = crlResource.openStream()) {
            crl = CertificateFactory.getInstance("X.509").generateCRL(is);
        }
        cut.refresh(List.of(crl));

        // Load client certificate and CA to build chain
        URL certResource = this.getClass().getResource("/crls/cert-client-valid.pem");
        assertThat(certResource).isNotNull();
        X509Certificate clientCert;
        try (InputStream is = certResource.openStream()) {
            clientCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        URL caResource = this.getClass().getResource("/crls/ca.pem");
        assertThat(caResource).isNotNull();
        X509Certificate caCert;
        try (InputStream is = caResource.openStream()) {
            caCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        X509Certificate[] chain = new X509Certificate[] { clientCert, caCert };

        // Verify that the non-revoked certificate is accepted (using empty CRL, cert not revoked yet)
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA")).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", sslEngine)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", socket)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA")).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", sslEngine)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", socket)).doesNotThrowAnyException();
    }

    @Test
    void should_accept_valid_certificate_with_crl_containing_other_revoked_certs() throws Exception {
        // Load truststore containing CA certificate
        URL truststoreResource = this.getClass().getResource("/crls/ca-truststore.jks");
        assertThat(truststoreResource).isNotNull();
        KeyStore trustStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_JKS, truststoreResource.getPath(), PASSWORD);
        cut.refresh(trustStore);

        // Load CRL containing revoked certificates (but not the valid client cert we're testing with)
        URL crlResource = this.getClass().getResource("/crls/crl-with-revocations.pem");
        assertThat(crlResource).isNotNull();
        CRL crl;
        try (InputStream is = crlResource.openStream()) {
            crl = CertificateFactory.getInstance("X.509").generateCRL(is);
        }
        cut.refresh(List.of(crl));

        // Load valid client certificate (not revoked)
        URL validCertResource = this.getClass().getResource("/crls/cert-client-valid.pem");
        assertThat(validCertResource).isNotNull();
        X509Certificate validClientCert;
        try (InputStream is = validCertResource.openStream()) {
            validClientCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        // Load CA certificate to build complete chain
        URL caResource = this.getClass().getResource("/crls/ca.pem");
        assertThat(caResource).isNotNull();
        X509Certificate caCert;
        try (InputStream is = caResource.openStream()) {
            caCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        X509Certificate[] chain = new X509Certificate[] { validClientCert, caCert };

        // Verify that a valid certificate is accepted even when CRL contains other revoked certificates
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA")).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", sslEngine)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", socket)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA")).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", sslEngine)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", socket)).doesNotThrowAnyException();
    }

    @Test
    void should_accept_certificate_when_no_crl_loaded() throws Exception {
        // Load truststore containing CA certificate
        URL truststoreResource = this.getClass().getResource("/crls/ca-truststore.jks");
        assertThat(truststoreResource).isNotNull();
        KeyStore trustStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_JKS, truststoreResource.getPath(), PASSWORD);
        cut.refresh(trustStore);

        // No CRL loaded - certificate should be accepted based on trust only

        // Load client certificate and CA to build chain
        URL certResource = this.getClass().getResource("/crls/cert-client-revoked.pem");
        assertThat(certResource).isNotNull();
        X509Certificate clientCert;
        try (InputStream is = certResource.openStream()) {
            clientCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        URL caResource = this.getClass().getResource("/crls/ca.pem");
        assertThat(caResource).isNotNull();
        X509Certificate caCert;
        try (InputStream is = caResource.openStream()) {
            caCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        X509Certificate[] chain = new X509Certificate[] { clientCert, caCert };

        // Verify that the certificate is accepted even without CRL
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA")).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", sslEngine)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkClientTrusted(chain, "RSA", socket)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA")).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", sslEngine)).doesNotThrowAnyException();
        assertThatCode(() -> cut.checkServerTrusted(chain, "RSA", socket)).doesNotThrowAnyException();
    }
}
