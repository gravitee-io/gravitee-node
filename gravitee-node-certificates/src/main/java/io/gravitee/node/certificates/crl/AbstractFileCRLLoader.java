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
package io.gravitee.node.certificates.crl;

import com.sun.nio.file.SensitivityWatchEventModifier;
import io.gravitee.node.api.certificate.CRLLoader;
import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.CustomLog;

/**
 * Abstract base class for file-based CRL loaders providing common functionality for watching and loading CRLs from the filesystem.
 * Subclasses must implement path validation, loading logic, and specify which paths should trigger reloads.
 *
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public abstract class AbstractFileCRLLoader implements CRLLoader {

    protected final String id;
    protected final Path path;
    protected final boolean watch;
    protected final ExecutorService executor;
    protected WatchService watchService;
    protected volatile boolean started;
    protected volatile Consumer<List<CRL>> handler;

    protected AbstractFileCRLLoader(String id, CRLLoaderOptions options, Path path) {
        this.id = id;
        this.path = path;
        this.watch = options.isWatch();
        this.executor = watch ? Executors.newSingleThreadExecutor(r -> new Thread(r, "gio.crl-watcher-" + id)) : null;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void start() {
        if (path == null) {
            log.warn("CRL path is null, skipping CRL loading");
            notifyHandler(Collections.emptyList());
            return;
        }

        log.debug("Initializing CRL loader [{}] for: {}, watch: {}", id, path, watch);

        if (Files.exists(path) && validatePath()) {
            load();
        } else {
            log.warn("CRL path does not exist yet: {}, will watch for its creation", path);
            notifyHandler(Collections.emptyList());
        }

        if (watch) {
            started = true;
            startWatchService();
        }
    }

    @Override
    public void stop() {
        if (!watch) {
            return;
        }

        started = false;
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("CRL loader [{}] executor did not terminate in time, forcing shutdown", id);
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("CRL loader [{}] interrupted while waiting for executor to terminate", id, e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("CRL loader [{}] error closing watch service", id, e);
            }
        }
    }

    @Override
    public void setEventHandler(@Nonnull Consumer<List<CRL>> handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    protected abstract boolean validatePath();

    protected abstract void load();

    protected abstract Path getWatchDirectory();

    protected abstract boolean shouldReload(Path changedPath);

    protected CRL loadSingleCRL(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCRL(in);
        } catch (Exception e) {
            log.debug("Unable to load CRL from file: {}, error: {}", file, e.getMessage());
            return null;
        }
    }

    protected void notifyHandler(List<CRL> crls) {
        if (handler != null) {
            handler.accept(crls);
        }
    }

    private void startWatchService() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path dirToWatch = getWatchDirectory();

            if (dirToWatch == null) {
                dirToWatch = Paths.get(".");
            }

            dirToWatch.register(
                watchService,
                new WatchEvent.Kind[] {
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                },
                SensitivityWatchEventModifier.HIGH
            );
            startWatch();
        } catch (IOException e) {
            log.error("CRL loader [{}] unable to watch path: {}", id, path, e);
        }
    }

    private void startWatch() {
        executor.execute(() -> {
            try {
                log.info("CRL loader [{}] start watching path: {}", id, path);
                WatchKey watchKey;
                while (started) {
                    watchKey = watchService.poll(200, TimeUnit.MILLISECONDS);
                    if (watchKey != null) {
                        boolean shouldReload = watchKey.pollEvents().stream().map(this::getPathFromWatchEvent).anyMatch(this::shouldReload);

                        if (shouldReload) {
                            log.info("CRL loader [{}] change detected, reloading", id);
                            load();
                        }

                        if (!watchKey.reset()) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException ie) {
                log.info("CRL loader [{}] watch has been stopped", id);
                Thread.currentThread().interrupt();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Path getPathFromWatchEvent(WatchEvent<?> watchEvent) {
        return ((WatchEvent<Path>) watchEvent).context();
    }
}
