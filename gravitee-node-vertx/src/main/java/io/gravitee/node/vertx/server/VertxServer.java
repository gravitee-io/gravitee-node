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

import io.gravitee.node.api.server.Server;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public abstract class VertxServer<T, C extends VertxServerOptions> implements Server<C> {

    protected final String id;
    protected final Vertx vertx;
    protected final VertxHttpServerOptions options;
    protected final List<HttpServer> delegates = new CopyOnWriteArrayList<>();

    @Override
    public String id() {
        return id;
    }

    public abstract T newInstance();

    public abstract List<T> instances();
}
