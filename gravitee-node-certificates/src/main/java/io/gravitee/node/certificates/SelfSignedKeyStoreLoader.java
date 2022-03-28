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
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SelfSignedKeyStoreLoader implements KeyStoreLoader {

    private static final Logger logger = LoggerFactory.getLogger(SelfSignedKeyStoreLoader.class);

    private final List<Consumer<KeyStoreBundle>> listeners;
    private final KeyStoreLoaderOptions options;

    public SelfSignedKeyStoreLoader(KeyStoreLoaderOptions options) {
        this.options = options;
        this.listeners = new ArrayList<>();
    }

    @Override
    public void start() {
        logger.debug("Initializing self-signed keystore certificate.");
        final String password = UUID.randomUUID().toString();
        final KeyStore keyStore = KeyStoreUtils.initSelfSigned("localhost", password);
        final KeyStoreBundle keyStoreBundle = new KeyStoreBundle(keyStore, password, options.getDefaultAlias());

        notifyListeners(keyStoreBundle);
    }

    @Override
    public void stop() {}

    @Override
    public void addListener(Consumer<KeyStoreBundle> listener) {
        listeners.add(listener);
    }

    private void notifyListeners(KeyStoreBundle keyStoreBundle) {
        listeners.forEach(consumer -> consumer.accept(keyStoreBundle));
    }
}
