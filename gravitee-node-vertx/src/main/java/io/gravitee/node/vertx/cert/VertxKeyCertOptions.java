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

import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import java.util.function.Function;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxKeyCertOptions implements KeyCertOptions {

    public static final Function<String, X509KeyManager> NULL_MAPPER_FUNCTION = s -> null;
    private final KeyManagerFactory keyManagerFactory;

    VertxKeyCertOptions(KeyManagerFactory keyManagerFactory) {
        if (keyManagerFactory == null || keyManagerFactory.getKeyManagers() == null || keyManagerFactory.getKeyManagers().length == 0) {
            throw new IllegalArgumentException("KeyManagerFactory is not present or is not initialized yet");
        }
        this.keyManagerFactory = keyManagerFactory;
    }

    VertxKeyCertOptions(X509KeyManager keyManager) {
        this(new VertxKeyManagerFactory(keyManager));
    }

    private VertxKeyCertOptions(VertxKeyCertOptions other) {
        this.keyManagerFactory = other.keyManagerFactory;
    }

    @Override
    public KeyCertOptions copy() {
        return new VertxKeyCertOptions(this);
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory(Vertx vertx) {
        return keyManagerFactory;
    }

    @Override
    public Function<String, X509KeyManager> keyManagerMapper(Vertx vertx) {
        // Return a mapper function to always return null. This force vertx to directly rely on our own KeyManagerFactory instead of recreating a new one for each server name.
        // This is mandatory since recent changes occurring internally in vertx SSLHelper (see https://github.com/eclipse-vertx/vert.x/pull/4468/files#diff-349d956034aca2f682714a50163bc83e32b0e5fa5f473840f2bca2d4049539deR344-R351)
        return NULL_MAPPER_FUNCTION;
    }
}
