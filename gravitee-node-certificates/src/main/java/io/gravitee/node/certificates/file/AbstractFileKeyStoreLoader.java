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
package io.gravitee.node.certificates.file;

import com.sun.nio.file.SensitivityWatchEventModifier;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreProcessingException;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import java.io.IOException;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class AbstractFileKeyStoreLoader<O extends AbstractStoreLoaderOptions> extends AbstractKeyStoreLoader<O> {

    private final ExecutorService executor;

    private final List<Path> filesToWatch;
    private boolean started;
    private boolean watching;

    private KeyStore keyStore;

    protected record LoadResult(KeyStore keyStore, List<Path> paths) {}

    protected AbstractFileKeyStoreLoader(O options) {
        super(options);
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "gio.file-cert-watcher"));
        this.filesToWatch = new ArrayList<>();
    }

    public void start() {
        log.debug("Initializing file keystore certificates.");
        load();
        started = true;
        if (options.isWatch() && !filesToWatch.isEmpty()) {
            try {
                WatchService watchService = prepareWatch();
                startWatch(watchService);
            } catch (IOException e) {
                log.error("Unable to watch the keystore files.", e);
            }
        }
    }

    public void stop() {
        started = false;
        executor.shutdown();
    }

    final void load() {
        if (options.getType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS) || options.getType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
            LoadResult loadResult = loadFromKeyStore();
            this.keyStore = loadResult.keyStore();
            setFilesToWatch(loadResult.paths());
        } else if (options.getType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
            LoadResult loadResult = loadFromPems();
            this.keyStore = loadResult.keyStore();
            setFilesToWatch(loadResult.paths());
        }

        if (keyStore != null) {
            onEvent(KeyStoreEvent.loadEvent(id(), keyStore, getPassword(), getDefaultAlias()));
        }
    }

    protected String getDefaultAlias() {
        return null;
    }

    protected abstract LoadResult loadFromPems();

    protected LoadResult loadFromKeyStore() {
        if (options.getPaths() != null && !options.getPaths().isEmpty()) {
            log.info("loading keystore from locations: {}", options.getPaths());
            List<Path> paths = options.getPaths().stream().map(FileSystems.getDefault()::getPath).toList();
            final List<KeyStore> localTrustStores = paths
                .stream()
                .map(path -> KeyStoreUtils.initFromPath(options.getType(), path.toString(), getPassword()))
                .toList();
            final KeyStore loaded;
            if (paths.size() == 1) {
                loaded = KeyStoreUtils.initFromPath(options.getType(), paths.get(0).toString(), getPassword());
            } else {
                loaded = KeyStoreUtils.merge(localTrustStores, getPassword());
            }
            return new LoadResult(loaded, paths);
        } else {
            throw new KeyStoreProcessingException(keyStoreLoadError());
        }
    }

    protected abstract String keyStoreLoadError();

    public boolean isWatching() {
        return watching;
    }

    private WatchService prepareWatch() throws IOException {
        final WatchService watcherService = FileSystems.getDefault().newWatchService();

        // Extract the list of path to watch from the list of files to watch (note: we can only watch folder, not files directly).
        final List<Path> watchedPaths = filesToWatch.stream().map(Path::getParent).distinct().toList();

        for (Path path : watchedPaths) {
            path.register(
                watcherService,
                new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_MODIFY },
                SensitivityWatchEventModifier.HIGH
            );
        }

        return watcherService;
    }

    private void startWatch(WatchService watcherService) {
        executor.execute(() -> {
            try {
                watching = true;
                log.info("Start watching files in: {}", filesToWatch.stream().map(Path::getParent).distinct().toList());
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
                log.info("Watch for keystore files has been stopped.");
            }
        });
    }

    private void setFilesToWatch(List<Path> paths) {
        if (filesToWatch.isEmpty()) {
            filesToWatch.addAll(paths);
        }
    }
}
