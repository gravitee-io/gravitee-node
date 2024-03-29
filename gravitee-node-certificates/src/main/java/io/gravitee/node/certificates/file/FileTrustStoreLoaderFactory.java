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

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactory;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileTrustStoreLoaderFactory implements KeyStoreLoaderFactory<TrustStoreLoaderOptions> {

    private static final List<String> SUPPORTED_TYPES = List.of(
        KeyStoreLoader.CERTIFICATE_FORMAT_JKS.toLowerCase(),
        KeyStoreLoader.CERTIFICATE_FORMAT_PEM.toLowerCase(),
        KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12.toLowerCase()
    );

    @Override
    public boolean canHandle(TrustStoreLoaderOptions options) {
        return (
            options.getType() != null &&
            SUPPORTED_TYPES.contains(options.getType().toLowerCase()) &&
            options.getPaths() != null &&
            !options.getPaths().isEmpty()
        );
    }

    @Override
    public KeyStoreLoader create(TrustStoreLoaderOptions options) {
        return new FileTrustStoreLoader(options);
    }
}
