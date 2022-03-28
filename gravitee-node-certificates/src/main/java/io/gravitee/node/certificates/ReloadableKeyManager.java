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

import static javax.net.ssl.StandardConstants.SNI_HOST_NAME;

import io.gravitee.common.util.KeyStoreUtils;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReloadableKeyManager extends X509ExtendedKeyManager {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableKeyManager.class);

    static final int MAX_SNI_DOMAINS = 10000;

    private String defaultAlias;

    private Map<String, String> sniDomainAliases;
    private volatile X509ExtendedKeyManager delegate;
    private boolean enableSni;

    public void load(String defaultAlias, KeyStore keyStore, String password, boolean enableSni) {
        try {
            this.enableSni = enableSni;

            // If no alias is defined, get it from keystore.
            if (defaultAlias == null) {
                defaultAlias = KeyStoreUtils.getDefaultAlias(keyStore);
            } else if (!keyStore.containsAlias(defaultAlias)) {
                throw new IllegalArgumentException(
                    String.format("Unable to load keystore, default alias [%s] not present in the keystore.", defaultAlias)
                );
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KeyStoreUtils.passwordToCharArray(password));

            this.defaultAlias = defaultAlias;

            if (enableSni) {
                // Try to pre-construct a list of domain -> alias to use for SNI.
                this.sniDomainAliases = new ConcurrentHashMap<>(KeyStoreUtils.getCommonNamesByAlias(keyStore));
            }
            this.delegate = (X509ExtendedKeyManager) keyManagerFactory.getKeyManagers()[0];

            logger.info("Key store has been (re)loaded with {} entries.", keyStore.size());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load keystore", e);
        }
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        if (!enableSni) {
            return defaultAlias;
        }

        final ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();
        final Optional<String> optionalSNIServerName = session
            .getRequestedServerNames()
            .stream()
            .filter(name -> name.getType() == SNI_HOST_NAME)
            .map(name -> ((SNIHostName) name).getAsciiName())
            .findFirst();

        if (optionalSNIServerName.isPresent()) {
            final String hostname = optionalSNIServerName.get();

            if (this.sniDomainAliases.containsKey(hostname)) {
                return this.sniDomainAliases.get(hostname);
            }

            // Try to find by wildcard.
            final Optional<Map.Entry<String, String>> optCN = sniDomainAliases
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("*."))
                .filter(e -> hostname.endsWith(e.getKey().substring(2)))
                .findFirst();

            if (optCN.isPresent()) {
                final String alias = optCN.get().getValue();
                cacheSniDomainAlias(hostname, alias);
                return alias;
            } else {
                // Add the hostname to avoid future resolutions.
                cacheSniDomainAlias(hostname, defaultAlias);
                return defaultAlias;
            }
        }

        return defaultAlias;
    }

    /**
     * Cache the mapping between domain name and certificate alias.
     * If the map is full, the cache put is ignored. This avoid to full the memory in case of dos attack on a lot of non unknown domain names.
     */
    private void cacheSniDomainAlias(String hostname, String alias) {
        if (sniDomainAliases.size() < MAX_SNI_DOMAINS) {
            this.sniDomainAliases.put(hostname, alias);
        }
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return delegate != null ? delegate.getServerAliases(keyType, issuers) : null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return delegate != null ? delegate.getCertificateChain(alias) : null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return delegate != null ? delegate.getPrivateKey(alias) : null;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return delegate != null ? delegate.getClientAliases(keyType, issuers) : null;
    }

    @Override
    public String chooseClientAlias(String[] aliases, Principal[] issuers, Socket socket) {
        return delegate != null ? delegate.chooseClientAlias(aliases, issuers, socket) : null;
    }

    @Override
    public String chooseServerAlias(String alias, Principal[] issuers, Socket socket) {
        return delegate != null ? delegate.chooseServerAlias(alias, issuers, socket) : null;
    }

    Map<String, String> getSniDomainAliases() {
        return sniDomainAliases;
    }

    void setSniDomainAliases(Map<String, String> sniDomainAliases) {
        this.sniDomainAliases = sniDomainAliases;
    }
}
