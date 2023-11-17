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
import io.vertx.core.net.TrustOptions;
import java.util.function.Function;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxTrustOptions implements TrustOptions {

    public static final Function<String, TrustManager[]> NULL_MAPPER_FUNCTION = s -> null;
    private final TrustManagerFactory trustManagerFactory;

    VertxTrustOptions(TrustManagerFactory trustManagerFactory) {
        if (
            trustManagerFactory == null ||
            trustManagerFactory.getTrustManagers() == null ||
            trustManagerFactory.getTrustManagers().length == 0
        ) {
            throw new IllegalArgumentException("TrustManagerFactory is not present or is not initialized yet");
        }
        this.trustManagerFactory = trustManagerFactory;
    }

    VertxTrustOptions(X509TrustManager trustManager) {
        this(new VertxTrustManagerFactory(trustManager));
    }

    private VertxTrustOptions(VertxTrustOptions other) {
        this.trustManagerFactory = other.trustManagerFactory;
    }

    @Override
    public TrustOptions copy() {
        return new VertxTrustOptions(this);
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory(Vertx vertx) throws Exception {
        return this.trustManagerFactory;
    }

    @Override
    public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) throws Exception {
        return NULL_MAPPER_FUNCTION;
    }
}
