/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.api.opentelemetry.http;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpRequestHead;
import io.vertx.core.net.SocketAddress;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public record ObservableHttpServerRequest(HttpServerRequest httpServerRequest) implements ObservableHttpRequest {
    /**
     * Only used for internal Vertx testing; see {@link HttpRequestHead#id() }
     */
    @Override
    public int id() {
        return 0;
    }

    @Override
    public String uri() {
        return httpServerRequest.uri();
    }

    @Override
    public String absoluteURI() {
        return httpServerRequest.absoluteURI();
    }

    @Override
    public HttpMethod method() {
        return httpServerRequest.method();
    }

    @Override
    public MultiMap headers() {
        return httpServerRequest.headers();
    }

    @Override
    public SocketAddress remoteAddress() {
        return httpServerRequest.remoteAddress();
    }
}
