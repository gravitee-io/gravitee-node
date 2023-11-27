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

import static javax.net.ssl.StandardConstants.SNI_HOST_NAME;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreProcessingException;
import io.gravitee.node.api.certificate.RefreshableX509Manager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RefreshableX509KeyManagerDelegator extends X509ExtendedKeyManager implements RefreshableX509Manager {

    private static final Logger logger = LoggerFactory.getLogger(RefreshableX509KeyManagerDelegator.class);

    static final int MAX_SNI_DOMAINS = 10000;
    private final Object mutex = new Object();
    private final String serverId;
    private final boolean sniEnabled;
    private KeyManagerDataHolder dataHolder;

    public RefreshableX509KeyManagerDelegator(String serverId, boolean sniEnabled) {
        this.serverId = serverId;
        this.sniEnabled = sniEnabled;
    }

    record KeyManagerDataHolder(String sniFallbackAlias, Map<String, String> domainToAliasMapping, X509ExtendedKeyManager keyManager) {}

    public void refresh(KeyStore keyStore, char[] password, String defaultAlias) {
        Objects.requireNonNull(keyStore, "cannot install null KeyStore");
        try {
            String sniFallbackAlias;
            if (defaultAlias == null) {
                sniFallbackAlias = KeyStoreUtils.getDefaultAlias(keyStore);
            } else if (!keyStore.containsAlias(defaultAlias)) {
                throw new IllegalArgumentException(
                    "Invalid configuration to load keystore, default alias [%s] not present in the keystore. server id: %s".formatted(
                            defaultAlias,
                            serverId
                        )
                );
            } else {
                sniFallbackAlias = defaultAlias;
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            final Map<String, String> newCommonNamesByAlias;
            if (sniEnabled) {
                // Try to pre-construct a list of domain -> alias to use for SNI.
                newCommonNamesByAlias = KeyStoreUtils.getCommonNamesByAlias(keyStore);
            } else {
                newCommonNamesByAlias = new HashMap<>();
            }

            X509ExtendedKeyManager keyManager = (X509ExtendedKeyManager) keyManagerFactory.getKeyManagers()[0];

            synchronized (mutex) {
                dataHolder = new KeyManagerDataHolder(sniFallbackAlias, new ConcurrentHashMap<>(newCommonNamesByAlias), keyManager);
            }

            logger.info("Key store has been (re)loaded with {} entries for server id: {}", keyStore.size(), serverId);
        } catch (Exception e) {
            throw new KeyStoreProcessingException("Unable to initialize key manager keystore", e);
        }
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        if (!sniEnabled) {
            return dataHolder.sniFallbackAlias();
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

            if (dataHolder.domainToAliasMapping().containsKey(hostname)) {
                return dataHolder.domainToAliasMapping().get(hostname);
            }

            // Try to find by wildcard.
            final Optional<Map.Entry<String, String>> optCN = dataHolder
                .domainToAliasMapping()
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
                cacheSniDomainAlias(hostname, dataHolder.sniFallbackAlias());
                return dataHolder.sniFallbackAlias();
            }
        }

        return dataHolder.sniFallbackAlias();
    }

    /**
     * Cache the mapping between domain name and certificate alias.
     * If the map is full, the cache put is ignored. This avoid to full the memory in case of dos attack on a lot of non unknown domain names.
     */
    private void cacheSniDomainAlias(String hostname, String alias) {
        if (dataHolder.domainToAliasMapping().size() < MAX_SNI_DOMAINS) {
            dataHolder.domainToAliasMapping().put(hostname, alias);
        }
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return dataHolder.keyManager() != null ? dataHolder.keyManager().getServerAliases(keyType, issuers) : null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return dataHolder.keyManager() != null ? dataHolder.keyManager().getCertificateChain(alias) : null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return dataHolder.keyManager() != null ? dataHolder.keyManager().getPrivateKey(alias) : null;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return dataHolder.keyManager() != null ? dataHolder.keyManager().getClientAliases(keyType, issuers) : null;
    }

    @Override
    public String chooseClientAlias(String[] aliases, Principal[] issuers, Socket socket) {
        return dataHolder.keyManager() != null ? dataHolder.keyManager().chooseClientAlias(aliases, issuers, socket) : null;
    }

    @Override
    public String chooseServerAlias(String alias, Principal[] issuers, Socket socket) {
        return dataHolder.keyManager() != null ? dataHolder.keyManager().chooseServerAlias(alias, issuers, socket) : null;
    }

    Map<String, String> getSniDomainAliases() {
        return dataHolder.domainToAliasMapping();
    }
}
