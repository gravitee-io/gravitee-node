package io.gravitee.node.certificates.crl;

import com.sun.nio.file.SensitivityWatchEventModifier;
import io.gravitee.node.api.certificate.CRLLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//TODO separate concerns, folder and file in different classes
public class FileCRLLoader implements CRLLoader {

    private final String id;
    private final ExecutorService executor;
    private WatchService watchService;
    private volatile boolean started;
    private final Path crlPath;
    private final boolean isDirectory;
    private volatile Consumer<List<CRL>> handler;

    public FileCRLLoader(String id, Path crlPath) {
        this.id = id;
        this.crlPath = crlPath;
        this.isDirectory = Files.isDirectory(crlPath);
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "gio.file-crl-watcher"));
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void start() {
        if (crlPath == null) {
            log.warn("CRL path is null, skipping CRL loading");
            return;
        }

        if (!Files.exists(crlPath)) {
            log.warn("CRL path does not exist: {}, skipping CRL loading", crlPath);
            notifyHandler(Collections.emptyList());
            return;
        }

        log.debug("Initializing file CRL watcher for: {} ({})", crlPath, isDirectory ? "directory" : "file");
        load();
        started = true;

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path dirToWatch = isDirectory ? crlPath : crlPath.getParent();

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
            startWatch(watchService);
        } catch (IOException e) {
            log.error("Unable to watch the CRL path: {}", crlPath, e);
        }
    }

    @Override
    public void stop() {
        started = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for executor to terminate", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Error closing watch service", e);
            }
        }
    }

    private void load() {
        if (!Files.exists(crlPath)) {
            log.warn("CRL path does not exist: {}", crlPath);
            notifyHandler(Collections.emptyList());
            return;
        }

        List<CRL> crls = isDirectory ? loadFromDirectory() : loadFromFile(crlPath);
        notifyHandler(crls);
    }

    private List<CRL> loadFromDirectory() {
        List<CRL> crls = new ArrayList<>();

        try (Stream<Path> paths = Files.list(crlPath)) {
            paths
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    CRL crl = loadSingleCRL(file);
                    if (crl != null) {
                        crls.add(crl);
                    }
                });
            log.debug("Loaded {} CRL(s) from directory: {}", crls.size(), crlPath);
        } catch (IOException e) {
            log.error("Unable to list CRL files in directory: {}", crlPath, e);
        }

        return crls;
    }

    private List<CRL> loadFromFile(Path file) {
        CRL crl = loadSingleCRL(file);
        if (crl != null) {
            log.info("Loaded CRL from file: {}", file);
            return List.of(crl);
        }
        return Collections.emptyList();
    }

    private CRL loadSingleCRL(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCRL(in);
        } catch (Exception e) {
            log.error("Unable to load CRL from file: {}, error: {}", file, e.getMessage());
            return null;
        }
    }

    private void notifyHandler(List<CRL> crls) {
        if (handler != null) {
            handler.accept(crls);
        }
    }

    @Override
    public void setEventHandler(@Nonnull Consumer<List<CRL>> handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    private void startWatch(WatchService watcherService) {
        executor.execute(() -> {
            try {
                log.info("Start watching CRL path: {} ({})", crlPath, isDirectory ? "directory" : "file");
                WatchKey watchKey;
                while (started) {
                    watchKey = watcherService.poll(200, TimeUnit.MILLISECONDS);
                    if (watchKey != null) {
                        final Optional<Path> optionalPath = watchKey
                            .pollEvents()
                            .stream()
                            .map(this::getPathFromWatchEvent)
                            .filter(watchGuard())
                            .findFirst();

                        if (optionalPath.isPresent()) {
                            log.info("CRL change detected: {}, reloading", optionalPath.get());
                            load();
                        }

                        if (!watchKey.reset()) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException ie) {
                log.info("Watch for CRL has been stopped.");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Path getPathFromWatchEvent(WatchEvent<?> watchEvent) {
        return ((WatchEvent<Path>) watchEvent).context();
    }

    protected Predicate<Path> watchGuard() {
        if (isDirectory) {
            // For directories, react to any file change in the directory
            return path -> true;
        } else {
            // For files, only react to changes to the specific file
            return path -> path.equals(crlPath.getFileName());
        }
    }
}
