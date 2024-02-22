/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * Base implementation for {@link KeyStoreLoaderManager} and {@link TrustStoreLoaderManager}.
 * ts core functions are to react to keystore (hence trustore) loading and unloading and update a main singleton instance of KeyStore that contains all aliases of all loaded keystore.
 * It scopes aliases by their loader ids to provide isolation and allow non destrcutive operations.
 * <p>This class is thread safe.</p>
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 *
 */
@Slf4j
public class AbstractKeyStoreLoaderManager {

    private final Map<String, KeyStoreLoader> loaders;
    private final KeyStoreLoader platformKeyStoreLoader;
    protected final RefreshableX509Manager refreshableX509Manager;
    private final String name;
    private char[] mainPassword;
    private KeyStore mainKeyStore;
    private KeyStore.PasswordProtection passwordProtection;

    private String platformKeyStoreLoaderId;

    private final AtomicBoolean started = new AtomicBoolean();
    private final List<KeyStoreLoader> queued = Collections.synchronizedList(new ArrayList<>());

    /**
     * @param target unique target name of the instance (for logging essentially)
     * @param platformKeyStoreLoader the startup keystore loader
     * @param refreshableX509Manager the holder of the KeyStore that will server certs and keys during TLS handshake
     */
    public AbstractKeyStoreLoaderManager(
        String target,
        KeyStoreLoader platformKeyStoreLoader,
        RefreshableX509Manager refreshableX509Manager
    ) {
        this.name = target;
        this.platformKeyStoreLoader = platformKeyStoreLoader;
        this.refreshableX509Manager = refreshableX509Manager;
        this.loaders = new ConcurrentHashMap<>();
    }

    /**
     * <p>Creates the main keystore. And register and starts the platform keystore loader.
     * This that can only be done once, that is why it is synchronized at the method level.
     * </p>
     * <p>
     * While this first invocation is being performed if any registration occurs via {@link #registerLoader(KeyStoreLoader)} (including platform keystore)
     * they are queued in a synchronized list and actual registration and start occurs sequentially at the very end of this method.
     * </p>
     * The queue is emptied and no longer used after that.
     * @throws KeyStoreException if the main keystore cannot be initialised
     * @throws CertificateException  if the main the keystore cannot be initialised
     * @throws IOException  if the main the keystore cannot be initialised
     * @throws NoSuchAlgorithmException  if the main the keystore cannot be initialised
     */
    public synchronized void start() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        if (started.get()) {
            return;
        }
        this.mainPassword = KeyStoreUtils.passwordToCharArray(UUID.randomUUID().toString());
        this.mainKeyStore = KeyStore.getInstance(KeyStoreUtils.DEFAULT_KEYSTORE_TYPE);
        this.mainKeyStore.load(null, mainPassword);
        this.passwordProtection = new KeyStore.PasswordProtection(mainPassword);
        this.platformKeyStoreLoaderId = platformKeyStoreLoader.id();
        this.queued.add(platformKeyStoreLoader);
        started.set(true);
        queued.forEach(this::registerLoader);
        queued.clear();
    }

    /**
     * Call {@link KeyStoreLoader#stop()} on all registered {@link KeyStoreLoader}
     */
    public void stop() {
        loaders.values().forEach(KeyStoreLoader::stop);
    }

    /**
     * Register a loader, call {@link KeyStoreLoader#setEventHandler(Consumer)} where the handler is this very class.
     * If {@link #start()} was not called yet then, the registration is queued and performed later.
     * <p>Eventually {@link KeyStoreLoader#start()} is called and the keystore is added to the main one a)in a thread safe fashion.</p>
     * <p>the event handler works as follows when an event is received:
     * <li>for {@link io.gravitee.node.api.certificate.KeyStoreEvent.LoadEvent}: removes all aliases matching {@link KeyStoreEvent#loaderId()} from the main keystore and add all aliases scopes with the loader id to the main keystore</li>
     * <li>for {@link io.gravitee.node.api.certificate.KeyStoreEvent.UnloadEvent}: removes all aliases matching {@link KeyStoreEvent#loaderId()} from the main keystore </li>
     * Then it clones the keystore and calls {@link RefreshableX509Manager#refresh(KeyStore, char[])} to make the keystore effective.
     * </p>
     * @param loader the {@link KeyStoreLoader} to register and eventually start
     */
    public final void registerLoader(final KeyStoreLoader loader) {
        if (!started.get()) {
            queued.add(loader);
            return;
        }
        log.info(
            "Register and start new keystore loader for target: {} of type: {} with id: {}",
            name,
            loader.getClass().getSimpleName(),
            loader.id()
        );
        loader.setEventHandler(keyStoreEvent -> {
            synchronized (refreshableX509Manager) {
                if (keyStoreEvent instanceof KeyStoreEvent.LoadEvent loadEvent) {
                    updateMain(loader, loadEvent);
                    refreshableX509Manager.refresh(clone(mainKeyStore), this.mainPassword);
                } else if (keyStoreEvent instanceof KeyStoreEvent.UnloadEvent unLoadEvent) {
                    removeKeyStore(unLoadEvent.loaderId());
                    refreshableX509Manager.refresh(clone(mainKeyStore), this.mainPassword);
                }
            }
        });
        loaders.put(loader.id(), loader);
        loader.start();
    }

    /**
     * Scopes an alias to the {@link KeyStoreLoader} id
     * @param idProvider an id provider, {@link KeyStoreLoader} in our case
     * @param currentAlias the existing alias
     * @return a scoped alias
     */
    protected String scopeAlias(IdProvider idProvider, String currentAlias) {
        if (currentAlias == null) {
            return null;
        }
        return idProvider.id() + ":" + currentAlias;
    }

    private void updateMain(IdProvider idProvider, KeyStoreEvent.LoadEvent keyStoreEvent) {
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

    private void updatePlatformKeyStore(IdProvider idProvider, KeyStoreEvent.LoadEvent keyStoreEvent) {
        removeKeyStore(this::isPlatformAlias, platformKeyStoreLoaderId, false);
        addKeyStore(idProvider, keyStoreEvent);
    }

    private boolean isPlatformAlias(String alias) {
        return isAliasOwnedByLoader(alias, platformKeyStoreLoaderId);
    }

    private void addKeyStore(IdProvider loader, KeyStoreEvent.LoadEvent event) {
        try {
            final KeyStore source = event.keyStore();
            for (String alias : Collections.list(source.aliases())) {
                String newAlias = scopeAlias(loader, alias);
                if (source.isKeyEntry(alias)) {
                    // Only key entries can be password protected.
                    KeyStore.Entry exisingEntry = source.getEntry(alias, event.passwordAsProtection());
                    mainKeyStore.setEntry(newAlias, exisingEntry, passwordProtection);
                } else {
                    KeyStore.Entry exisingEntry = source.getEntry(alias, null);
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

    private static boolean isAliasOwnedByLoader(String alias, String loaderId) {
        return alias.startsWith(loaderId);
    }

    private KeyStore clone(KeyStore source) {
        try {
            var destination = KeyStore.getInstance(KeyStoreUtils.DEFAULT_KEYSTORE_TYPE);
            destination.load(null, mainPassword);
            for (String alias : Collections.list(source.aliases())) {
                if (source.isKeyEntry(alias)) {
                    // Only key entries can be password protected.
                    final KeyStore.Entry entry = source.getEntry(alias, passwordProtection);
                    destination.setEntry(alias, entry, passwordProtection);
                } else {
                    final KeyStore.Entry entry = source.getEntry(alias, null);
                    destination.setEntry(alias, entry, null);
                }
            }
            return destination;
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException e) {
            throw new IllegalArgumentException("Unable to clone keystore", e);
        }
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
