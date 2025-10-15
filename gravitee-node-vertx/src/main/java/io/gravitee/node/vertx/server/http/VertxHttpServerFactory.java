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

import io.gravitee.node.api.certificate.CRLLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.api.server.ServerFactory;
import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.DefaultCRLLoaderFactoryRegistry;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.server.AbstractVertxServerFactory;
import io.vertx.rxjava3.core.Vertx;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerFactory extends AbstractVertxServerFactory implements ServerFactory<VertxHttpServer, VertxHttpServerOptions> {

    /**
     * Constructor without CRL support for backward compatibility.
     * CRL validation is not supported when using this constructor. Configuring CRL options will result in a startup failure.
     */
    public VertxHttpServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry
    ) {
        this(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry, new DefaultCRLLoaderFactoryRegistry());
    }

    public VertxHttpServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry,
        CRLLoaderFactoryRegistry crlLoaderFactoryRegistry
    ) {
        super(vertx, keyStoreLoaderFactoryRegistry, trustStoreLoaderFactoryRegistry, crlLoaderFactoryRegistry);
    }

    @Override
    public VertxHttpServer create(VertxHttpServerOptions options) {
        KeyStoreLoaderManager keyStoreLoaderManager = createKeyManager(options);
        TrustStoreLoaderManager trustStoreLoaderManager = createCertificateManager(options);
        CRLLoaderManager crlLoaderManager = createCRLManager(trustStoreLoaderManager, options.getCrlLoaderOptions());
        return new VertxHttpServer(vertx, options, keyStoreLoaderManager, trustStoreLoaderManager, crlLoaderManager);
    }
}
