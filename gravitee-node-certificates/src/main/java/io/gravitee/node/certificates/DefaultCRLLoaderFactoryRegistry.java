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

import io.gravitee.node.api.certificate.CRLLoader;
import io.gravitee.node.api.certificate.CRLLoaderFactory;
import io.gravitee.node.api.certificate.CRLLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.security.cert.CRL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.CustomLog;

/**
 * Default implementation of {@link CRLLoaderFactoryRegistry} that maintains a registry of CRL loader factories
 * and creates appropriate loaders based on provided options. If no suitable factory is found or options are not
 * configured, a no-op loader is returned.
 *
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DefaultCRLLoaderFactoryRegistry implements CRLLoaderFactoryRegistry {

    private static final CRLLoader NO_OP_CRL_LOADER = new NoOpCRLLoader();
    private final Set<CRLLoaderFactory> factories = new HashSet<>();

    @Override
    public void registerFactory(CRLLoaderFactory crlLoaderFactory) {
        log.debug("Registering CRL loader factory: {}", crlLoaderFactory.getClass().getSimpleName());
        factories.add(crlLoaderFactory);
    }

    @Override
    public Set<CRLLoaderFactory> getLoaderFactories() {
        return Set.copyOf(factories);
    }

    @Override
    public CRLLoader createLoader(CRLLoaderOptions options) {
        if (options == null || !options.isConfigured()) {
            log.debug("CRL options not configured, returning no-op CRL loader");
            return NO_OP_CRL_LOADER;
        }

        List<CRLLoaderFactory> matchingFactories = getLoaderFactories().stream().filter(factory -> factory.canHandle(options)).toList();

        if (matchingFactories.isEmpty()) {
            throw new IllegalArgumentException("No CRL loader factory found for path: %s".formatted(options.getPath()));
        }

        if (matchingFactories.size() > 1) {
            throw new IllegalArgumentException("Multiple CRL loader factories found for path: %s".formatted(options.getPath()));
        }

        CRLLoaderFactory factory = matchingFactories.get(0);
        log.info("Creating CRL loader using factory: {}", factory.getClass().getSimpleName());
        return factory.create(options);
    }

    static class NoOpCRLLoader implements CRLLoader {

        @Override
        public String id() {
            return "no-op-crl-loader";
        }

        @Override
        public void start() {
            // no op
        }

        @Override
        public void stop() {
            // no op
        }

        @Override
        public void setEventHandler(Consumer<List<CRL>> handler) {
            // no op
        }
    }
}
