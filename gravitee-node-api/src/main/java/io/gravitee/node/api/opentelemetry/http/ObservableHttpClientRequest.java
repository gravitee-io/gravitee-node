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
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.HttpRequestHead;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public class ObservableHttpClientRequest implements ObservableHttpRequest {

    @NonNull
    private final RequestOptions requestOptions;

    private HttpClientRequest httpClientRequest;

    /**
     * Only used for internal Vertx testing; see {@link HttpRequestHead#id() }
     */
    @Override
    public int id() {
        return 0;
    }

    @Override
    public String uri() {
        return requestOptions.getURI();
    }

    @Override
    public String absoluteURI() {
        return (
            (Boolean.TRUE.equals(requestOptions.isSsl()) ? "https://" : "http://") +
            requestOptions.getHost() +
            ':' +
            requestOptions.getPort() +
            requestOptions.getURI()
        );
    }

    @Override
    public HttpMethod method() {
        return requestOptions.getMethod();
    }

    @Override
    public MultiMap headers() {
        MultiMap headers = null;
        if (httpClientRequest != null) {
            headers = httpClientRequest.headers();
        }
        if (headers == null) {
            headers = requestOptions.getHeaders();
        }
        if (headers == null) {
            return MultiMap.caseInsensitiveMultiMap();
        }
        return headers;
    }

    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = null;
        if (httpClientRequest != null) {
            remoteAddress = httpClientRequest.connection().remoteAddress();
        }
        if (remoteAddress == null) {
            remoteAddress = (SocketAddress) requestOptions.getServer();
        }
        return remoteAddress;
    }

    public String traceOperation() {
        String traceOperation = null;
        if (httpClientRequest != null) {
            traceOperation = httpClientRequest.traceOperation();
        }
        if (traceOperation == null) {
            traceOperation = requestOptions.getTraceOperation();
        }
        return traceOperation;
    }
}
