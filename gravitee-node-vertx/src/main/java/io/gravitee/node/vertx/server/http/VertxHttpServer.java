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

import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.cert.VertxKeyCertOptions;
import io.gravitee.node.vertx.cert.VertxTrustOptions;
import io.gravitee.node.vertx.server.VertxServer;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import java.util.Collections;
import java.util.List;
import lombok.CustomLog;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class VertxHttpServer extends VertxServer<HttpServer, VertxHttpServerOptions> {

    public static final String KIND = "http";

    public VertxHttpServer(
        Vertx vertx,
        VertxHttpServerOptions options,
        KeyStoreLoaderManager keyStoreLoaderManager,
        TrustStoreLoaderManager trustStoreLoaderManager,
        CRLLoaderManager crlLoaderManager
    ) {
        super(vertx, options, keyStoreLoaderManager, trustStoreLoaderManager, crlLoaderManager);
    }

    @Override
    public String type() {
        return KIND;
    }

    @Override
    public HttpServer newInstance() {
        startKeyStoreManagers();
        final HttpServer httpServer = vertx.createHttpServer(
            options.createHttpServerOptions(
                new VertxKeyCertOptions(keyStoreLoaderManager.getKeyManager()),
                new VertxTrustOptions(trustStoreLoaderManager.getCertificateManager())
            )
        );
        delegates.add(httpServer);
        return httpServer;
    }

    @Override
    public List<HttpServer> instances() {
        return Collections.unmodifiableList(delegates);
    }

    @Override
    public VertxHttpServerOptions options() {
        return options;
    }
}
