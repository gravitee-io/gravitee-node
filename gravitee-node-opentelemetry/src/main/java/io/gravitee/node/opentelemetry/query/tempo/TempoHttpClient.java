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
package io.gravitee.node.opentelemetry.query.tempo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.CustomLog;

/**
 * Reactive Vert.x wrapper around Tempo's HTTP API. The underlying {@link HttpClient} is built by {@code VertxHttpClientFactory}
 * with host/port/SSL/proxy already configured, so requests carry only the relative URI.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class TempoHttpClient implements AutoCloseable {

    private final HttpClient httpClient;
    private final Map<String, String> staticHeaders;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public TempoHttpClient(final HttpClient httpClient, final Map<String, String> staticHeaders) {
        this.httpClient = httpClient;
        this.staticHeaders = staticHeaders == null ? Map.of() : Map.copyOf(staticHeaders);
    }

    public Single<TempoTraceResponse> getTrace(final String traceId) {
        return executeGet("/api/traces/" + URLEncoder.encode(traceId, StandardCharsets.UTF_8), TempoTraceResponse.class);
    }

    public Single<TempoSearchResponse> searchTracesTraceQL(final String traceQL, final Integer limit, final Long start, final Long end) {
        StringBuilder uri = new StringBuilder("/api/search");
        String separator = "?";

        if (traceQL != null) {
            uri.append(separator).append("q=").append(URLEncoder.encode(traceQL, StandardCharsets.UTF_8));
            separator = "&";
        }
        if (limit != null) {
            uri.append(separator).append("limit=").append(limit);
            separator = "&";
        }
        if (start != null) {
            uri.append(separator).append("start=").append(start);
            separator = "&";
        }
        if (end != null) {
            uri.append(separator).append("end=").append(end);
        }

        return executeGet(uri.toString(), TempoSearchResponse.class);
    }

    /**
     * Closes the underlying Vert.x {@link HttpClient}. Subscribed eagerly because the close result is not awaited — the bean
     * lifecycle hook only triggers the close, it does not block on its completion.
     */
    @Override
    public void close() {
        httpClient.close().subscribe();
    }

    private <T> Single<T> executeGet(final String requestUri, final Class<T> responseType) {
        return httpClient
            .rxRequest(HttpMethod.GET, requestUri)
            .map(req -> {
                req.putHeader("Accept", "application/json");
                staticHeaders.forEach(req::putHeader);
                return req;
            })
            .flatMap(req -> req.rxSend())
            .flatMap(resp -> {
                int status = resp.statusCode();
                if (status < 200 || status >= 300) {
                    // Strip the query string from the logged path: encoded TraceQL can carry user-controlled tag values that
                    // do not belong in operational logs.
                    log.warn("Tempo API returned HTTP {} for {}", status, pathOnly(requestUri));
                    return Single.error(new TempoClientException("Tempo API returned HTTP " + status));
                }
                return resp.body();
            })
            .map(buffer -> objectMapper.readValue(buffer.getBytes(), responseType));
    }

    private static String pathOnly(final String requestUri) {
        int q = requestUri.indexOf('?');
        return q < 0 ? requestUri : requestUri.substring(0, q);
    }

    public static class TempoClientException extends RuntimeException {

        public TempoClientException(final String message) {
            super(message);
        }
    }
}
