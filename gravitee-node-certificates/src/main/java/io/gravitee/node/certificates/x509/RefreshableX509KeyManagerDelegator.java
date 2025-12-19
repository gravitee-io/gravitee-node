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
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;
import lombok.CustomLog;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RefreshableX509KeyManagerDelegator extends X509ExtendedKeyManager implements RefreshableX509Manager {

    static final int MAX_SNI_DOMAINS = 10000;
    private final String target;
    private final boolean sniEnabled;
    private String defaultAlias;
    private KeyManagerDataHolder dataHolder;

    public RefreshableX509KeyManagerDelegator(String name, boolean sniEnabled) {
        this.target = Objects.requireNonNull(name, "target cannot be null");
        this.sniEnabled = sniEnabled;
    }

    public void setDefaultAlias(String defaultAlias) {
        this.defaultAlias = defaultAlias;
    }

    record KeyManagerDataHolder(String sniFallbackAlias, Map<String, String> domainToAliasMapping, X509ExtendedKeyManager keyManager) {}

    public void refresh(KeyStore keyStore, char[] password) {
        Objects.requireNonNull(keyStore, "cannot install null KeyStore");
        try {
            String sniFallbackAlias;
            if (defaultAlias == null) {
                sniFallbackAlias = KeyStoreUtils.getDefaultAlias(keyStore);
            } else if (!keyStore.containsAlias(defaultAlias)) {
                throw new IllegalArgumentException(
                    "Invalid configuration to load keystore, default alias [%s] not present in the keystore. target: %s".formatted(
                            defaultAlias,
                            target
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

            dataHolder = new KeyManagerDataHolder(sniFallbackAlias, new ConcurrentHashMap<>(newCommonNamesByAlias), keyManager);

            log.info("Key store has been (re)loaded with {} entries for target: {}", keyStore.size(), target);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new KeyStoreProcessingException("Unable to initialize key manager keystore", e);
        }
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        // keep the ref local as we have many things to do they need to use the same instance
        KeyManagerDataHolder localDataHolder = this.dataHolder;

        if (!sniEnabled) {
            return localDataHolder.sniFallbackAlias();
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

            if (localDataHolder.domainToAliasMapping().containsKey(hostname)) {
                return localDataHolder.domainToAliasMapping().get(hostname);
            }

            // Try to find by wildcard.
            final Optional<Map.Entry<String, String>> optCN = localDataHolder
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
                cacheSniDomainAlias(hostname, localDataHolder.sniFallbackAlias());
                return localDataHolder.sniFallbackAlias();
            }
        }

        return localDataHolder.sniFallbackAlias();
    }

    /**
     * Cache the mapping between domain name and certificate alias.
     * If the map is full, the cache put is ignored. This avoids to full the memory in case of dos attack on a lot of unknown domain names.
     */
    private void cacheSniDomainAlias(String hostname, String alias) {
        KeyManagerDataHolder localDataHolder = this.dataHolder;
        if (localDataHolder.domainToAliasMapping().size() < MAX_SNI_DOMAINS) {
            localDataHolder.domainToAliasMapping().put(hostname, alias);
        }
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        KeyManagerDataHolder localDataHolder = this.dataHolder;
        return localDataHolder.keyManager() != null ? localDataHolder.keyManager().getServerAliases(keyType, issuers) : null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        KeyManagerDataHolder localDataHolder = this.dataHolder;
        return localDataHolder.keyManager() != null ? localDataHolder.keyManager().getCertificateChain(alias) : null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        KeyManagerDataHolder localDataHolder = this.dataHolder;
        return localDataHolder.keyManager() != null ? localDataHolder.keyManager().getPrivateKey(alias) : null;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        KeyManagerDataHolder localDataHolder = this.dataHolder;
        return localDataHolder.keyManager() != null ? localDataHolder.keyManager().getClientAliases(keyType, issuers) : null;
    }

    @Override
    public String chooseClientAlias(String[] aliases, Principal[] issuers, Socket socket) {
        KeyManagerDataHolder localDataHolder = this.dataHolder;
        return localDataHolder.keyManager() != null ? localDataHolder.keyManager().chooseClientAlias(aliases, issuers, socket) : null;
    }

    @Override
    public String chooseServerAlias(String alias, Principal[] issuers, Socket socket) {
        KeyManagerDataHolder localDataHolder = this.dataHolder;
        return localDataHolder.keyManager() != null ? localDataHolder.keyManager().chooseServerAlias(alias, issuers, socket) : null;
    }

    Map<String, String> getSniDomainAliases() {
        return dataHolder.domainToAliasMapping();
    }
}
