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

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileKeyStoreLoader extends AbstractFileKeyStoreLoader<KeyStoreLoaderOptions, KeyStoreBundle> implements KeyStoreLoader {

    private KeyStoreBundle keyStoreBundle;

    public FileKeyStoreLoader(KeyStoreLoaderOptions options) {
        super(options);
    }

    public void load() {
        if (
            options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS) ||
            options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)
        ) {
            setFilesToWatch(loadFromKeyStore());
        } else if (options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
            setFilesToWatch(loadFromPems());
        } else {
            throw new IllegalArgumentException(String.format("Unsupported keystore format (%s).", options.getKeyStoreType()));
        }

        if (keyStoreBundle != null) {
            notifyListeners(keyStoreBundle);
        }
    }

    private List<Path> loadFromKeyStore() {
        final List<Path> paths = new ArrayList<>();

        if (options.getKeyStorePath() != null && !options.getKeyStorePath().isEmpty()) {
            final KeyStore keyStore = KeyStoreUtils.initFromPath(
                options.getKeyStoreType(),
                options.getKeyStorePath(),
                options.getKeyStorePassword()
            );
            keyStoreBundle = new KeyStoreBundle(keyStore, options.getKeyStorePassword(), options.getDefaultAlias());
            paths.add(FileSystems.getDefault().getPath(options.getKeyStorePath()));
        } else {
            throw new IllegalArgumentException("JKS/PKCS12 Keystore is required but was not specified. Unable to configure TLS.");
        }
        return paths;
    }

    private List<Path> loadFromPems() {
        final List<Path> paths = new ArrayList<>();

        if (options.getKeyStoreCertificates() != null && !options.getKeyStoreCertificates().isEmpty()) {
            final List<String> certs = options.getKeyStoreCertificates().stream().map(CertificateOptions::getCertificate).toList();
            final List<String> keys = options.getKeyStoreCertificates().stream().map(CertificateOptions::getPrivateKey).toList();
            final KeyStore keyStore = KeyStoreUtils.initFromPems(certs, keys, options.getKeyStorePassword());

            // No default alias can be provided when loading keystore from pems / keys.
            keyStoreBundle = new KeyStoreBundle(keyStore, options.getKeyStorePassword(), null);

            certs.forEach(cert -> paths.add(FileSystems.getDefault().getPath(cert)));
            keys.forEach(key -> paths.add(FileSystems.getDefault().getPath(key)));
        } else {
            throw new IllegalArgumentException("A PEM Keystore is missing. Unable to configure TLS.");
        }
        return paths;
    }
}
