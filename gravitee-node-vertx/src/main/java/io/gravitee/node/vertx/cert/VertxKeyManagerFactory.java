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
package io.gravitee.node.vertx.cert;

import java.security.KeyStore;
import java.security.Provider;
import java.util.Objects;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxKeyManagerFactory extends KeyManagerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxKeyManagerFactory.class);
    private static final String KEY_MANAGER_FACTORY_ALGORITHM = "no-algorithm";
    private static final Provider PROVIDER = new Provider("", 1.0, "") {};

    VertxKeyManagerFactory(KeyManager keyManager) {
        super(new VertxKeyManagerFactory.KeyManagerFactorySpiWrapper(keyManager), PROVIDER, KEY_MANAGER_FACTORY_ALGORITHM);
    }

    private static class KeyManagerFactorySpiWrapper extends KeyManagerFactorySpi {

        private final KeyManager[] keyManagers;

        private KeyManagerFactorySpiWrapper(KeyManager keyManager) {
            Objects.requireNonNull(keyManager);
            this.keyManagers = new KeyManager[] { keyManager };
        }

        @Override
        protected void engineInit(KeyStore keyStore, char[] keyStorePassword) {
            LOGGER.info("Ignoring provided KeyStore");
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
            LOGGER.info("Ignoring provided ManagerFactoryParameters");
        }

        @Override
        protected KeyManager[] engineGetKeyManagers() {
            return keyManagers;
        }
    }
}
