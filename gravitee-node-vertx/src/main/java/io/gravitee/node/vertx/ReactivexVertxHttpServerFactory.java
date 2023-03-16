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
package io.gravitee.node.vertx;

import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.vertx.configuration.ListenerConfiguration;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactivexVertxHttpServerFactory extends AbstractVertxHttpServerFactory<HttpServer[]> {

    private final Vertx vertx;

    @Autowired
    public ReactivexVertxHttpServerFactory(
        Vertx vertx,
        ListenerConfiguration listenerConfiguration,
        KeyStoreLoaderManager keyStoreLoaderManager
    ) {
        super(listenerConfiguration, keyStoreLoaderManager);
        this.vertx = vertx;
    }

    @Override
    public HttpServer[] getObject() throws Exception {
        HttpServerOptions[] httpServerOptions = getHttpServerOptions();
        HttpServer[] httpServers = new HttpServer[httpServerOptions.length];

        for (int i = 0; i < httpServerOptions.length; i++) {
            httpServers[i] = VertxHttpServerProvider.create(vertx, httpServerOptions[i]);
        }

        return httpServers;
    }

    @Override
    public Class<?> getObjectType() {
        return HttpServer.class;
    }

    @Override
    public boolean isSingleton() {
        // Scope is managed indirectly by Vertx verticle.
        return false;
    }
}
