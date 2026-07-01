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
package io.gravitee.node.certificates;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@RunWith(MockitoJUnitRunner.class)
public class CertificateExpiryUtilsTest {

    @Mock
    private Logger log;

    private KeyPair keyPair;

    @Before
    public void before() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @Test
    public void should_warn_when_certificate_expires_within_threshold() throws Exception {
        KeyStore keyStore = keyStoreWith("expiring", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS));

        CertificateExpiryUtils.warnIfExpired(keyStore, log);

        verify(log).warn(contains("expires in"), eq("expiring"), anyLong(), any());
    }

    @Test
    public void should_log_error_when_certificate_already_expired() throws Exception {
        KeyStore keyStore = keyStoreWith("expired", Instant.now().minus(20, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS));

        CertificateExpiryUtils.warnIfExpired(keyStore, log);

        verify(log).error(contains("EXPIRED"), eq("expired"), any());
    }

    @Test
    public void should_warn_when_certificate_not_yet_valid() throws Exception {
        KeyStore keyStore = keyStoreWith("future", Instant.now().plus(5, ChronoUnit.DAYS), Instant.now().plus(100, ChronoUnit.DAYS));

        CertificateExpiryUtils.warnIfExpired(keyStore, log);

        verify(log).warn(contains("not yet valid"), eq("future"), any());
    }

    @Test
    public void should_not_warn_when_certificate_valid_for_a_long_time() throws Exception {
        KeyStore keyStore = keyStoreWith("healthy", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS));

        CertificateExpiryUtils.warnIfExpired(keyStore, log);

        verify(log, never()).warn(any(String.class), any(), any(), any());
        verify(log, never()).warn(any(String.class), any(), any());
        verify(log, never()).error(any(String.class), any(), any());
    }

    @Test
    public void should_warn_for_collection_of_certificates() throws Exception {
        var cert = certificate("expiring", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS));

        CertificateExpiryUtils.warnIfExpired(List.of(cert), "pem-source", log);

        verify(log).warn(contains("expires in"), contains("pem-source"), anyLong(), any());
    }

    private KeyStore keyStoreWith(String alias, Instant notBefore, Instant notAfter) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(
            alias,
            keyPair.getPrivate(),
            "secret".toCharArray(),
            new java.security.cert.Certificate[] { certificate(alias, notBefore, notAfter) }
        );
        return keyStore;
    }

    private java.security.cert.X509Certificate certificate(String cn, Instant notBefore, Instant notAfter) throws Exception {
        X500Principal subject = new X500Principal("CN=" + cn);
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
}
