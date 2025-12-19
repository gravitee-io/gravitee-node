package io.gravitee.node.certificates.file;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreProcessingException;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.CustomLog;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class FolderTrustStoreLoader extends AbstractFileKeyStoreLoader<TrustStoreLoaderOptions> implements KeyStoreLoader {

    public FolderTrustStoreLoader(TrustStoreLoaderOptions options) {
        super(options);
    }

    public LoadResult loadFromPems() {
        if (options.getPaths() != null && !options.getPaths().isEmpty()) {
            log.info("loading truststore from folders : {}", options.getPaths());

            List<Path> directories = options.getPaths().stream().map(FileSystems.getDefault()::getPath).toList();
            if (directories.stream().anyMatch(directory -> !Files.isDirectory(directory))) {
                throw new KeyStoreProcessingException("Some paths specified to load PEM files are not directories");
            }

            List<Path> allFiles = new ArrayList<>();
            for (Path directory : directories) {
                try (Stream<Path> files = Files.list(directory)) {
                    files.filter(Files::isRegularFile).forEach(allFiles::add);
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot list file while search for PEM files", e);
                }
            }

            KeyStore trustStore = KeyStoreUtils.initFromPemCertificateFiles(allFiles.stream().map(Path::toString).toList(), getPassword());

            return new LoadResult(trustStore, allFiles);
        } else {
            throw new KeyStoreProcessingException("PEM files are required but path was not specified. Unable to configure mutual TLS.");
        }
    }

    @Override
    protected Predicate<Path> watchGuard() {
        // we are watching all files in the directory, allowing new certs to be discovered
        return path -> true;
    }

    @Override
    protected String getKeyStoreLoadingErrorMessage() {
        throw new IllegalStateException("this method should never be called");
    }
}
