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

import static io.gravitee.node.api.certificate.KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.common.util.KeyStoreUtils;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
    void should_load_truststore_store_and_trust_certs() throws CertificateException, IOException {
        KeyStore trustStore = loadTruststore();
        cut.refresh(trustStore);
        assertThat(cut.getAcceptedIssuers()).hasSize(2);
        try (var is = this.getClass().getResource("/truststores/client2.crt").openStream();) {
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
        try (var is = this.getClass().getResource("/truststores/client1.crt").openStream();) {
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

    private KeyStore loadTruststore() {
        return KeyStoreUtils.initFromPath(
            CERTIFICATE_FORMAT_PKCS12,
            this.getClass().getResource("/truststores/truststore2-3.p12").getPath(),
            PASSWORD
        );
    }
}
