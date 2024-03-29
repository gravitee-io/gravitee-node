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
package io.gravitee.node.vertx.server.http;

import io.gravitee.node.api.certificate.KeyStoreLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.api.server.ServerFactory;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.server.AbstractVertxServerFactory;
import io.vertx.rxjava3.core.Vertx;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerFactory extends AbstractVertxServerFactory implements ServerFactory<VertxHttpServer, VertxHttpServerOptions> {

    public VertxHttpServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry
    ) {
        super(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry);
    }

    @Override
    public VertxHttpServer create(VertxHttpServerOptions options) {
        KeyStoreLoaderManager keyStoreLoaderManager = createKeyManager(options);
        TrustStoreLoaderManager trustStoreLoaderManager = createCertificateManager(options);
        return new VertxHttpServer(vertx, options, keyStoreLoaderManager, trustStoreLoaderManager);
    }
}
