package io.gravitee.node.certificates;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.TrustStoreLoader;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileTrustStoreLoader extends AbstractFileKeyStoreLoader<TrustStoreLoaderOptions, KeyStore> implements TrustStoreLoader {

    private KeyStore trustStore;

    public FileTrustStoreLoader(TrustStoreLoaderOptions options) {
        super(options);
    }

    public void load() {
        if (
            options.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS) ||
            options.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)
        ) {
            setFilesToWatch(loadFromKeyStore());
        } else if (options.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
            setFilesToWatch(loadFromPems());
        } else {
            throw new IllegalArgumentException(String.format("Unsupported truststore format (%s).", options.getTrustStoreType()));
        }

        if (trustStore != null) {
            notifyListeners(trustStore);
        }
    }

    private List<Path> loadFromKeyStore() {
        if (options.getTrustStorePaths() != null) {
            List<Path> paths;
            paths = options.getTrustStorePaths().stream().map(FileSystems.getDefault()::getPath).toList();
            final List<KeyStore> localTrustStores = paths
                .stream()
                .map(path -> KeyStoreUtils.initFromPath(options.getTrustStoreType(), path.toString(), options.getTrustStorePassword()))
                .toList();
            trustStore = KeyStoreUtils.merge(localTrustStores, options.getTrustStorePassword());
            return paths;
        } else {
            throw new IllegalArgumentException("JKS/PKCS12 Keystore is required but was not specified. Unable to configure Mutual TLS.");
        }
    }

    private List<Path> loadFromPems() {
        if (options.getTrustStorePaths() != null) {
            this.trustStore = KeyStoreUtils.initFromPemCertificateFiles(options.getTrustStorePaths(), options.getTrustStorePassword());
            return options.getTrustStorePaths().stream().map(FileSystems.getDefault()::getPath).toList();
        } else {
            throw new IllegalArgumentException("A PEM Keystore is missing. Unable to configure mTLS.");
        }
    }
}
