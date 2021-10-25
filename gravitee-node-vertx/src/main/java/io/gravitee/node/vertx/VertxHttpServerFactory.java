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

import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerFactory implements FactoryBean<HttpServer> {

    private final Vertx vertx;
    private final HttpServerConfiguration httpServerConfiguration;

    @Autowired
    public VertxHttpServerFactory(Vertx vertx, HttpServerConfiguration httpServerConfiguration) {
        this.vertx = vertx;
        this.httpServerConfiguration = httpServerConfiguration;
    }

    @Override
    public HttpServer getObject() throws Exception {
        return VertxHttpServerProvider.create(vertx, httpServerConfiguration.getHttpServerOptions());
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
