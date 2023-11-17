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

import com.sun.nio.file.SensitivityWatchEventModifier;
import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractFileKeyStoreLoader<O extends AbstractStoreLoaderOptions, K> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFileKeyStoreLoader.class);
    private final List<Consumer<K>> listeners;
    private final ExecutorService executor;
    protected final O options;
    private final List<Path> filesToWatch;
    private boolean started;
    private boolean watching;

    protected AbstractFileKeyStoreLoader(O options) {
        this.options = options;
        this.listeners = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "gio.file-cert-watcher"));
        this.filesToWatch = new ArrayList<>();
    }

    public void start() {
        logger.debug("Initializing file keystore certificates.");
        load();
        started = true;
        if (options.isWatch() && !filesToWatch.isEmpty()) {
            try {
                WatchService watchService = prepareWatch();
                startWatch(watchService);
            } catch (IOException e) {
                logger.error("Unable to watch the keystore files.", e);
            }
        }
    }

    public void stop() {
        started = false;
        executor.shutdown();
    }

    protected abstract void load();

    public final void addListener(Consumer<K> listener) {
        this.listeners.add(listener);
    }

    public final void notifyListeners(K keyStore) {
        listeners.forEach(consumer -> consumer.accept(keyStore));
    }

    public boolean isWatching() {
        return watching;
    }

    protected void setFilesToWatch(List<Path> paths) {
        if (filesToWatch.isEmpty()) {
            filesToWatch.addAll(paths);
        }
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
            }
        });
    }
}
