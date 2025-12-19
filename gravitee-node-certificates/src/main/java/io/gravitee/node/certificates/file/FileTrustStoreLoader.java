package io.gravitee.node.certificates.file;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreProcessingException;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.CustomLog;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class FileTrustStoreLoader extends AbstractFileKeyStoreLoader<TrustStoreLoaderOptions> implements KeyStoreLoader {

    public FileTrustStoreLoader(TrustStoreLoaderOptions options) {
        super(options);
    }

    public LoadResult loadFromPems() {
        if (options.getPaths() != null) {
            log.info("loading truststore from pem locations: {}", options.getPaths());
            KeyStore trustStore = KeyStoreUtils.initFromPemCertificateFiles(options.getPaths(), getPassword());
            List<Path> paths = options.getPaths().stream().map(FileSystems.getDefault()::getPath).toList();
            return new LoadResult(trustStore, paths);
        } else {
            throw new KeyStoreProcessingException("PEM files are required but path was not specified. Unable to configure mutual TLS.");
        }
    }

    @Override
    protected String getKeyStoreLoadingErrorMessage() {
        return "JKS/PKCS12 Keystore is required but path was not specified. Unable to configure mutual TLS.";
    }
}
