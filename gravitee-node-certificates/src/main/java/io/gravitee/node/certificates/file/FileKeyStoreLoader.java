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

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreProcessingException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class FileKeyStoreLoader extends AbstractFileKeyStoreLoader<KeyStoreLoaderOptions> implements KeyStoreLoader {

    public FileKeyStoreLoader(KeyStoreLoaderOptions options) {
        super(options);
    }

    @Override
    protected String keyStoreLoadError() {
        return "JKS/PKCS12 Keystore is required but path was not specified. Unable to configure TLS.";
    }

    public LoadResult loadFromPems() {
        if (options.getCertificates() != null && !options.getCertificates().isEmpty()) {
            log.info("loading keystore from pem locations: {}", options.getCertificates());
            final List<Path> paths = new ArrayList<>();
            final List<String> certs = options.getCertificates().stream().map(CertificateOptions::getCertificate).toList();
            final List<String> keys = options.getCertificates().stream().map(CertificateOptions::getPrivateKey).toList();
            var keyStore = KeyStoreUtils.initFromPems(certs, keys, getPassword());
            certs.forEach(cert -> paths.add(FileSystems.getDefault().getPath(cert)));
            keys.forEach(key -> paths.add(FileSystems.getDefault().getPath(key)));
            return new LoadResult(keyStore, paths);
        } else {
            throw new KeyStoreProcessingException("PEM files are required but path was not specified. Unable to configure TLS.");
        }
    }

    @Override
    protected String getDefaultAlias() {
        return options.getDefaultAlias();
    }
}
