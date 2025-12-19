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

import io.gravitee.node.api.certificate.CRLRefreshable;
import io.gravitee.node.api.certificate.RefreshableX509Manager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.CustomLog;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RefreshableX509TrustManagerDelegator extends X509ExtendedTrustManager implements RefreshableX509Manager, CRLRefreshable {

    private final String target;
    private X509ExtendedTrustManager delegate;
    private volatile List<CRL> crls = List.of();

    public RefreshableX509TrustManagerDelegator(String target) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
    }

    @Override
    public void refresh(KeyStore keyStore, char[] empty) {
        refresh(keyStore);
    }

    public void refresh(KeyStore keyStore) {
        Objects.requireNonNull(keyStore, "cannot install null KeyStore");
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            this.delegate = (X509ExtendedTrustManager) trustManagerFactory.getTrustManagers()[0];

            log.info("Trust store has been (re)loaded with {} entries for target: {}", keyStore.size(), target);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create trust manager for target: %s".formatted(target), e);
        }
    }

    @Override
    public void refresh(List<CRL> crls) {
        if (crls != null) {
            this.crls = List.copyOf(crls);
            log.info("CRL has been (re)loaded with {} entries for target: {}", crls.size(), target);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.delegate;
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkClientTrusted(chain, authType);
        }
    }

    private void checkRevoked(X509Certificate[] x509Certificates) throws CertificateException {
        for (X509Certificate cert : x509Certificates) {
            for (CRL crl : crls) {
                if (crl.isRevoked(cert)) {
                    throw new CertificateException(
                        "Certificate with serial number " +
                        cert.getSerialNumber() +
                        " and subject '" +
                        cert.getSubjectX500Principal() +
                        "' is revoked."
                    );
                }
            }
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.delegate;
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509ExtendedTrustManager trustManager = this.delegate;
        if (trustManager != null) {
            return trustManager.getAcceptedIssuers();
        }
        return new X509Certificate[] {};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.delegate;
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkClientTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.delegate;
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkServerTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.delegate;
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkClientTrusted(chain, authType, engine);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.delegate;
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkServerTrusted(chain, authType, engine);
        }
    }
}
