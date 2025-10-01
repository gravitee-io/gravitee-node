package io.gravitee.node.certificates.crl;

import io.gravitee.node.api.certificate.CRLLoader;
import io.gravitee.node.api.certificate.CRLLoaderFactory;
import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileCRLLoaderFactory implements CRLLoaderFactory {

    @Override
    public boolean canHandle(CRLLoaderOptions options) {
        return options != null && options.getPath() != null && !options.getPath().isEmpty();
    }

    @Override
    public CRLLoader create(CRLLoaderOptions options) {
        if (options.getPath() == null || options.getPath().isEmpty()) {
            throw new IllegalArgumentException("CRL file path is required");
        }

        String pathStr = options.getPath();
        Path crlPath = Paths.get(pathStr);

        log.debug("Creating file CRL loader for path: {}", crlPath);
        return new FileCRLLoader("file-crl:" + pathStr, crlPath);
    }
}
