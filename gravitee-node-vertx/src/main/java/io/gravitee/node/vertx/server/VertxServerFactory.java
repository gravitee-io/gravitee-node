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
package io.gravitee.node.vertx.server;

import io.gravitee.node.api.certificate.CRLLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.api.server.ServerFactory;
import io.gravitee.node.vertx.server.http.VertxHttpServerFactory;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.gravitee.node.vertx.server.tcp.VertxTcpServerFactory;
import io.gravitee.node.vertx.server.tcp.VertxTcpServerOptions;
import io.vertx.rxjava3.core.Vertx;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxServerFactory<T extends VertxServer<?, C>, C extends VertxServerOptions> implements ServerFactory<T, C> {

    private final VertxHttpServerFactory httpServerFactory;
    private final VertxTcpServerFactory tcpServerFactory;

    /**
     * Constructor without CRL support for backward compatibility.
     * CRL validation is not supported when using this constructor. Configuring CRL options will result in a startup failure.
     */
    public VertxServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry
    ) {
        this.httpServerFactory = new VertxHttpServerFactory(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry);
        this.tcpServerFactory = new VertxTcpServerFactory(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry);
    }

    public VertxServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry,
        CRLLoaderFactoryRegistry crlLoaderFactoryRegistry
    ) {
        this.httpServerFactory =
            new VertxHttpServerFactory(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry, crlLoaderFactoryRegistry);
        this.tcpServerFactory =
            new VertxTcpServerFactory(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry, crlLoaderFactoryRegistry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T create(C options) {
        if (options instanceof VertxHttpServerOptions http) {
            return (T) httpServerFactory.create(http);
        }
        if (options instanceof VertxTcpServerOptions tcp) {
            return (T) tcpServerFactory.create(tcp);
        }

        throw new IllegalArgumentException(
            "Server type is not a supported vertx server (option class=[" + options.getClass().getSimpleName() + "])"
        );
    }
}
