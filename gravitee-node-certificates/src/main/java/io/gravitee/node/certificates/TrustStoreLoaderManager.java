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

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.certificate.TrustStoreLoader;
import io.gravitee.node.api.certificate.TrustStoreLoaderFactory;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TrustStoreLoaderManager extends AbstractService<TrustStoreLoaderManager> {

    private final Set<TrustStoreLoaderFactory> loaderFactories;
    private final Set<TrustStoreLoader> loaders;

    public TrustStoreLoaderManager() {
        this.loaderFactories = new HashSet<>();
        this.loaders = new HashSet<>();

        // Automatically register the file keystore nd self-signed loader factories.
        this.registerFactory(new FileTrustStoreLoaderFactory());
    }

    @Override
    public TrustStoreLoaderManager preStop() throws Exception {
        loaders.forEach(TrustStoreLoader::stop);
        return this;
    }

    public void registerFactory(TrustStoreLoaderFactory keyStoreLoaderFactory) {
        loaderFactories.add(keyStoreLoaderFactory);
    }

    public Set<TrustStoreLoaderFactory> getLoaderFactories() {
        return loaderFactories;
    }

    public TrustStoreLoader create(TrustStoreLoaderOptions options, String serverId) {
        return getLoaderFactories()
            .stream()
            .filter(factory -> factory.canHandle(options))
            .findFirst()
            .map(factory -> factory.create(options, serverId))
            .orElse(null);
    }
}
