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

import io.gravitee.node.api.certificate.RefreshableX509Manager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RefreshableX509TrustManagerDelegator extends X509ExtendedTrustManager implements RefreshableX509Manager {

    private final String serverId;
    private static final Logger logger = LoggerFactory.getLogger(RefreshableX509TrustManagerDelegator.class);

    private final Object mutex = new Object();
    private X509ExtendedTrustManager delegate;

    public RefreshableX509TrustManagerDelegator(String serverId) {
        this.serverId = Objects.requireNonNull(serverId, "serverId cannot be null");
    }

    @Override
    public void refresh(KeyStore keyStore, char[] empty, String ignore) {
        refresh(keyStore);
    }

    public void refresh(KeyStore keyStore) {
        Objects.requireNonNull(keyStore, "cannot install null KeyStore");
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            synchronized (mutex) {
                this.delegate = (X509ExtendedTrustManager) trustManagerFactory.getTrustManagers()[0];
            }

            logger.info("Trust store has been (re)loaded with {} entries for server [{}]", keyStore.size(), serverId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create trust manager for server is: %s".formatted(serverId), e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (delegate != null) {
            delegate.checkClientTrusted(chain, authType);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (delegate != null) {
            delegate.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return (delegate != null) ? delegate.getAcceptedIssuers() : null;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (delegate != null) {
            delegate.checkClientTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (delegate != null) {
            delegate.checkServerTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        if (delegate != null) {
            delegate.checkClientTrusted(chain, authType, engine);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        if (delegate != null) {
            delegate.checkServerTrusted(chain, authType, engine);
        }
    }
}
