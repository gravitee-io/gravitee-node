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
import java.util.Set;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultKeyStoreLoaderFactoryRegistry<O extends AbstractStoreLoaderOptions> implements KeyStoreLoaderFactoryRegistry<O> {

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
    public KeyStoreLoader createLoader(O options, String serverId) {
        return getLoaderFactories()
            .stream()
            .filter(keyStoreLoaderFactory -> keyStoreLoaderFactory.canHandle(options))
            .findFirst()
            .map(keyStoreLoaderFactory -> keyStoreLoaderFactory.create(options))
            .orElse(new NoOpKeyStoreLoader<>(options));
    }

    static class NoOpKeyStoreLoader<O extends AbstractStoreLoaderOptions> extends AbstractKeyStoreLoader<O> {

        NoOpKeyStoreLoader(O options) {
            super(options);
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
        public String id() {
            return "no-op";
        }
    }
}
