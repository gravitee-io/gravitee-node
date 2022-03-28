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

import static java.nio.file.StandardWatchEventKinds.*;

import com.sun.nio.file.SensitivityWatchEventModifier;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileKeyStoreLoader implements KeyStoreLoader {

    private static final Logger logger = LoggerFactory.getLogger(FileKeyStoreLoader.class);

    private final KeyStoreLoaderOptions options;
    private final List<Consumer<KeyStoreBundle>> listeners;
    private final ExecutorService executor;
    private KeyStoreBundle keyStoreBundle;
    private List<Path> filesToWatch;
    private boolean started;
    private boolean watching;

    public FileKeyStoreLoader(KeyStoreLoaderOptions options) {
        this.options = options;
        this.listeners = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "gio.file-cert-watcher"));
        this.filesToWatch = new ArrayList<>();
    }

    @Override
    public void start() {
        logger.debug("Initializing file keystore certificates.");

        load();

        started = true;

        if (options.isWatch()) {
            startWatch();
        }
    }

    @Override
    public void stop() {
        started = false;

        executor.shutdown();
    }

    private void load() {
        if (
            options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS) ||
            options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)
        ) {
            filesToWatch = loadFromKeyStore();
        } else if (options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
            filesToWatch = loadFromPems();
        } else if (options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_SELF_SIGNED)) {
            filesToWatch = loadFromSelfSigned();
        } else {
            throw new IllegalArgumentException(String.format("Unsupported keystore format (%s).", options.getKeyStoreType()));
        }

        if (keyStoreBundle != null) {
            notifyListeners(keyStoreBundle);
        }
    }

    private List<Path> loadFromKeyStore() {
        final List<Path> paths = new ArrayList<>();

        if (options.getKeyStorePath() != null && !options.getKeyStorePath().isEmpty()) {
            final KeyStore keyStore = KeyStoreUtils.initFromPath(
                options.getKeyStoreType(),
                options.getKeyStorePath(),
                options.getKeyStorePassword()
            );
            keyStoreBundle = new KeyStoreBundle(keyStore, options.getKeyStorePassword(), options.getDefaultAlias());
            paths.add(FileSystems.getDefault().getPath(options.getKeyStorePath()));
        } else {
            throw new IllegalArgumentException("A JKS/PKCS12 Keystore is missing. Unable to configure TLS.");
        }
        return paths;
    }

    private List<Path> loadFromPems() {
        final List<Path> paths = new ArrayList<>();

        if (options.getKeyStoreCertificates() != null && !options.getKeyStoreCertificates().isEmpty()) {
            final List<String> certs = options
                .getKeyStoreCertificates()
                .stream()
                .map(CertificateOptions::getCertificate)
                .collect(Collectors.toList());
            final List<String> keys = options
                .getKeyStoreCertificates()
                .stream()
                .map(CertificateOptions::getPrivateKey)
                .collect(Collectors.toList());
            final KeyStore keyStore = KeyStoreUtils.initFromPems(certs, keys, options.getKeyStorePassword());

            // No default alias can be provided when loading keystore from pems / keys.
            keyStoreBundle = new KeyStoreBundle(keyStore, options.getKeyStorePassword(), null);

            certs.forEach(cert -> paths.add(FileSystems.getDefault().getPath(cert)));
            keys.forEach(key -> paths.add(FileSystems.getDefault().getPath(key)));
        } else {
            throw new IllegalArgumentException("A PEM Keystore is missing. Unable to configure TLS.");
        }
        return paths;
    }

    private List<Path> loadFromSelfSigned() {
        keyStoreBundle = null;
        return new ArrayList<>();
    }

    @Override
    public void addListener(Consumer<KeyStoreBundle> listener) {
        listeners.add(listener);
    }

    private void startWatch() {
        executor.execute(() -> {
            try {
                WatchService watcherService = FileSystems.getDefault().newWatchService();

                // Extract the list of path to watch from the list of files to watch (note: we can only watch folder, not files directly).
                final List<Path> watchedPaths = filesToWatch.stream().map(Path::getParent).distinct().collect(Collectors.toList());

                for (Path path : watchedPaths) {
                    path.register(
                        watcherService,
                        new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_MODIFY },
                        SensitivityWatchEventModifier.HIGH
                    );
                }

                watching = true;

                WatchKey watchKey;
                while (started) {
                    watchKey = watcherService.poll(200, TimeUnit.MILLISECONDS);
                    if (watchKey != null) {
                        final Optional<Path> optionalPath = watchKey
                            .pollEvents()
                            .stream()
                            .map(watchEvent -> ((WatchEvent<Path>) watchEvent).context())
                            .filter(file -> filesToWatch.stream().map(Path::getFileName).anyMatch(file::equals))
                            .findFirst();

                        if (optionalPath.isPresent()) {
                            // In case of any changes, just reload the complete keystore.
                            load();
                        }

                        if (!watchKey.reset()) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException ie) {
                logger.info("Watch for keystore files has been stopped.");
            } catch (Exception e) {
                logger.error("Unable to watch the keystore files.", e);
            }
        });
    }

    private void notifyListeners(KeyStoreBundle keyStoreBundle) {
        listeners.forEach(consumer -> consumer.accept(keyStoreBundle));
    }

    public boolean isWatching() {
        return watching;
    }
}
