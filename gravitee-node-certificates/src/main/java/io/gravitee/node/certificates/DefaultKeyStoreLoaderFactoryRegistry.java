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

/**
 * Default implementation of {@link KeyStoreLoaderFactory} interface, for Javadoc see interface
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
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
        return factories
            .stream()
            .findFirst()
            .map(keyStoreLoaderFactory -> keyStoreLoaderFactory.create(options))
            .orElse(NO_OP_KEY_STORE_LOADER);
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
