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

import io.gravitee.node.api.certificate.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.CustomLog;

/**
 * Default implementation of {@link KeyStoreLoaderFactory} interface, for Javadoc see interface
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DefaultKeyStoreLoaderFactoryRegistry<O extends AbstractStoreLoaderOptions> implements KeyStoreLoaderFactoryRegistry<O> {

    protected static final NoOpKeyStoreLoader NO_OP_KEY_STORE_LOADER = new NoOpKeyStoreLoader();
    private final Set<KeyStoreLoaderFactory<O>> loaderFactories;

    public DefaultKeyStoreLoaderFactoryRegistry() {
        this.loaderFactories = new HashSet<>();
    }

    @Override
    public void registerFactory(KeyStoreLoaderFactory<O> keyStoreLoaderFactory) {
        loaderFactories.add(keyStoreLoaderFactory);
    }

    @Override
    public Set<KeyStoreLoaderFactory<O>> getLoaderFactories() {
        return loaderFactories;
    }

    /**
     * Whether the options point at anything to load from. The fields are the ones every store shares; the
     * per-format extras (certificates, self-signed) are matched by their own factory, so a configuration
     * carrying one of those never reaches this test.
     *
     * <p>Deliberately not {@code TrustStoreLoaderOptions#isConfigured()}: that one looks at paths alone,
     * and reusing it here would treat a secret or Kubernetes location as nothing at all.
     */
    private static boolean namesASource(final AbstractStoreLoaderOptions options) {
        return (
            options != null &&
            options.getType() != null &&
            (
                (options.getPaths() != null && !options.getPaths().isEmpty()) ||
                options.getSecretLocation() != null ||
                (options.getKubernetesLocations() != null && !options.getKubernetesLocations().isEmpty())
            )
        );
    }

    @Override
    public KeyStoreLoader createLoader(O options) {
        List<KeyStoreLoaderFactory<O>> factories = getLoaderFactories()
            .stream()
            .filter(keyStoreLoaderFactory -> keyStoreLoaderFactory.canHandle(options))
            .toList();

        if (factories.size() > 1) {
            throw new IllegalArgumentException(
                "KeyStore or TrustStore options are not properly set. Several ways where found to load a keystore, there can only be one. Options were: %s".formatted(
                        options
                    )
            );
        }
        if (factories.isEmpty()) {
            if (!namesASource(options)) {
                // The common case, not a misconfiguration: createLoader is called for every server whether or not
                // it is secured, and the options are always built, with a default type and nothing to load from.
                log.debug("No store configured, returning a no-op loader");
            } else {
                // Something was configured and no source claimed it — most often a type that is only wired for
                // some of them. Left silent, this is an empty store on a server that starts anyway.
                log.warn(
                    "No loader accepted the store configuration, no certificate will be loaded. type={}, paths={}, secretLocation={}, kubernetesLocations={}",
                    options.getType(),
                    options.getPaths(),
                    options.getSecretLocation(),
                    options.getKubernetesLocations()
                );
            }
            return NO_OP_KEY_STORE_LOADER;
        }
        return factories.get(0).create(options);
    }

    /**
     * When no factory is found this class is returned signifying that nothing will be done. This allows to always have a {@link KeyStoreLoader} instead of dealing with null
     */
    static class NoOpKeyStoreLoader implements KeyStoreLoader {

        @Override
        public void start() {
            // no op
        }

        @Override
        public void stop() {
            // no op
        }

        @Override
        public void setEventHandler(Consumer<KeyStoreEvent> handler) {}

        @Override
        public String id() {
            return "no-op";
        }
    }
}
