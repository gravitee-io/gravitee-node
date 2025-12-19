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
package io.gravitee.node.certificates.selfsigned;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import java.security.KeyStore;
import lombok.CustomLog;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class SelfSignedKeyStoreLoader extends AbstractKeyStoreLoader<KeyStoreLoaderOptions> {

    public SelfSignedKeyStoreLoader() {
        super(KeyStoreLoaderOptions.builder().build());
    }

    @Override
    public void start() {
        log.debug("Initializing self-signed keystore certificate.");
        final KeyStore keyStore = KeyStoreUtils.initSelfSigned("localhost", getPassword());
        String loaderId = id();
        onEvent(new KeyStoreEvent.LoadEvent(loaderId, keyStore, getPassword()));
    }

    @Override
    public void stop() {
        // nothing to stop
    }
}
