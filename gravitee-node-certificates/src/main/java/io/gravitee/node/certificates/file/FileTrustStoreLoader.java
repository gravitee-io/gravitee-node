package io.gravitee.node.certificates.file;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreProcessingException;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileTrustStoreLoader extends AbstractFileKeyStoreLoader<TrustStoreLoaderOptions> implements KeyStoreLoader {

    public FileTrustStoreLoader(TrustStoreLoaderOptions options) {
        super(options);
    }

    public LoadResult loadFromPems() {
        if (options.getPaths() != null) {
            KeyStore trustStore = KeyStoreUtils.initFromPemCertificateFiles(options.getPaths(), getPassword());
            List<Path> paths = options.getPaths().stream().map(FileSystems.getDefault()::getPath).toList();
            return new LoadResult(trustStore, paths);
        } else {
            throw new KeyStoreProcessingException("A PEM Keystore is missing. Unable to configure mutual TLS.");
        }
    }

    @Override
    protected String keyStoreLoadError() {
        return "JKS/PKCS12 Keystore is required but was not specified. Unable to configure mutual TLS.";
    }
}
