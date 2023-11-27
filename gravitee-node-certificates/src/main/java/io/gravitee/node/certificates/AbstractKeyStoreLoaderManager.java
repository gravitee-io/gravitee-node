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

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.*;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class AbstractKeyStoreLoaderManager {

    private final Map<String, KeyStoreLoader> loaders;
    private final KeyStoreLoader platformKeyStoreLoader;
    protected final RefreshableX509Manager refreshableX509Manager;
    private char[] mainPassword;
    private KeyStore mainKeyStore;
    private KeyStore.PasswordProtection passwordProtection;

    private String platformKeyStoreLoaderId;

    public AbstractKeyStoreLoaderManager(KeyStoreLoader platformKeyStoreLoader, RefreshableX509Manager refreshableX509Manager) {
        this.platformKeyStoreLoader = platformKeyStoreLoader;
        this.loaders = new ConcurrentHashMap<>();
        this.refreshableX509Manager = refreshableX509Manager;
    }

    protected boolean requirePassword() {
        return true;
    }

    public void start() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        this.mainPassword = KeyStoreUtils.passwordToCharArray(UUID.randomUUID().toString());
        this.mainKeyStore = KeyStore.getInstance(KeyStoreUtils.DEFAULT_KEYSTORE_TYPE);
        this.mainKeyStore.load(null, mainPassword);
        this.passwordProtection = new KeyStore.PasswordProtection(mainPassword);
        this.platformKeyStoreLoaderId = platformKeyStoreLoader.id();

        registerLoader(platformKeyStoreLoader);
    }

    public void registerLoader(final KeyStoreLoader loader) {
        loader.setEventHandler(keyStoreEvent -> {
            if (keyStoreEvent.type() == KeyStoreEvent.EventType.LOAD) {
                synchronized (loaders) {
                    updateMain(loader, keyStoreEvent);
                    refreshableX509Manager.refresh(mainKeyStore, this.mainPassword, keyStoreEvent.defaultAlias());
                }
            } else if (keyStoreEvent.type() == KeyStoreEvent.EventType.UNLOAD) {
                synchronized (loaders) {
                    removeKeyStore(keyStoreEvent.loaderId());
                    refreshableX509Manager.refresh(mainKeyStore, this.mainPassword, keyStoreEvent.defaultAlias());
                }
            }
        });
        loaders.put(loader.id(), loader);
        loader.start();
    }

    private void updateMain(IdProvider idProvider, KeyStoreEvent keyStoreEvent) {
        try {
            if (idProvider.id().equals(platformKeyStoreLoaderId) && mainKeyStore.size() > 0) {
                updatePlatformKeyStore(idProvider, keyStoreEvent);
            } else {
                addKeyStore(idProvider, keyStoreEvent);
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreProcessingException("cannot read keystore", e);
        }
    }

    private void updatePlatformKeyStore(IdProvider idProvider, KeyStoreEvent keyStoreEvent) {
        removeKeyStore(this::isPlatformAlias, platformKeyStoreLoaderId, false);
        addKeyStore(idProvider, keyStoreEvent);
    }

    private void addKeyStore(IdProvider loader, KeyStoreEvent event) {
        try {
            for (String alias : Collections.list(event.keyStore().aliases())) {
                String newAlias = makeNewAlias(loader, alias);
                if (requirePassword()) {
                    KeyStore.Entry exisingEntry = event.keyStore().getEntry(alias, event.passwordAsProtection());
                    mainKeyStore.setEntry(newAlias, exisingEntry, passwordProtection);
                } else {
                    KeyStore.Entry exisingEntry = event.keyStore().getEntry(alias, null);
                    mainKeyStore.setEntry(newAlias, exisingEntry, null);
                }
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            throw new KeyStoreProcessingException("cannot add entry to keystore", e);
        }
    }

    private void removeKeyStore(String loaderId) {
        removeKeyStore(alias -> isAliasOwnedByLoader(alias, loaderId), loaderId, true);
    }

    private boolean isPlatformAlias(String alias) {
        return isAliasOwnedByLoader(alias, platformKeyStoreLoaderId);
    }

    private static boolean isAliasOwnedByLoader(String alias, String loaderId) {
        return alias.startsWith(loaderId);
    }

    private String makeNewAlias(IdProvider idProvider, String currentAlias) {
        return idProvider.id() + ":" + currentAlias;
    }

    private void removeKeyStore(Predicate<String> aliasRemovePredicate, String loaderId, boolean removeLoader) {
        try {
            for (String alias : aliases()) {
                if (aliasRemovePredicate.test(alias)) {
                    mainKeyStore.deleteEntry(alias);
                }
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreProcessingException("could not remove entry from KeyStore", e);
        } finally {
            if (removeLoader) {
                loaders.remove(loaderId);
            }
        }
    }

    public void stop() {
        loaders.values().forEach(KeyStoreLoader::stop);
    }

    @VisibleForTesting
    Map<String, KeyStoreLoader> loaders() {
        return Collections.unmodifiableMap(loaders);
    }

    @VisibleForTesting
    List<String> aliases() throws KeyStoreException {
        return Collections.unmodifiableList(Collections.list(mainKeyStore.aliases()));
    }
}
