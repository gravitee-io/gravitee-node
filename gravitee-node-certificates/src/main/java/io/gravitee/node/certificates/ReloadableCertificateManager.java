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

import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReloadableCertificateManager extends X509ExtendedTrustManager {

    private final String serverId;

    private static final Logger logger = LoggerFactory.getLogger(ReloadableCertificateManager.class);

    private final AtomicReference<X509ExtendedTrustManager> delegate = new AtomicReference<>();

    public ReloadableCertificateManager(String serverId) {
        this.serverId = Objects.requireNonNull(serverId, "serverId cannot be null");
    }

    public void load(KeyStore keyStore) {
        Objects.requireNonNull(keyStore, "cannot set null KeyStore");
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            this.delegate.set((X509ExtendedTrustManager) trustManagerFactory.getTrustManagers()[0]);

            logger.info("Trust store has been (re)loaded with {} entries for server id: {}", keyStore.size(), serverId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create trust manager for server is: %s".formatted(serverId), e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (delegate.get() != null) {
            delegate.get().checkClientTrusted(chain, authType);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (delegate.get() != null) {
            delegate.get().checkServerTrusted(chain, authType);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return (delegate.get() != null) ? delegate.get().getAcceptedIssuers() : null;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (delegate.get() != null) {
            delegate.get().checkClientTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (delegate.get() != null) {
            delegate.get().checkServerTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        if (delegate.get() != null) {
            delegate.get().checkClientTrusted(chain, authType, engine);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        if (delegate.get() != null) {
            delegate.get().checkServerTrusted(chain, authType, engine);
        }
    }
}
