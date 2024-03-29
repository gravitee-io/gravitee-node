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

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.certificates.x509.RefreshableX509TrustManagerDelegator;
import javax.net.ssl.X509TrustManager;

/**
 * This class manages the unique {@link java.security.KeyStore} for trusting client certificates during TLS handshake (a.k.a truststore).
 * It provides the {@link X509TrustManager} to be used by the server to do so.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TrustStoreLoaderManager extends AbstractKeyStoreLoaderManager {

    public TrustStoreLoaderManager(String target, KeyStoreLoader platformKeyStoreLoader) {
        super(target, platformKeyStoreLoader, new RefreshableX509TrustManagerDelegator(target));
    }

    /**
     *
     * @return JDK {@link javax.net.ssl.TrustManager}
     */
    public X509TrustManager getCertificateManager() {
        return (X509TrustManager) refreshableX509Manager;
    }
}
