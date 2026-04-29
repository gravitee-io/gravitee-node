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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.query.tempo.TempoHttpClient.TempoClientException;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.node.vertx.client.http.VertxHttpClientOptions;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.junit5.VertxExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TempoHttpClientTest {

    private Vertx vertx;
    private HttpServer server;
    private TempoHttpClient client;
    private final Queue<RecordedRequest> recorded = new ConcurrentLinkedQueue<>();
    private volatile Consumer<HttpServerRequest> stubHandler = req -> req.response().setStatusCode(200).end("{}");

    @BeforeEach
    void start_server() throws InterruptedException {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        server.requestHandler(req -> {
            recorded.add(new RecordedRequest(req.method().name(), req.uri(), copyHeaders(req)));
            stubHandler.accept(req);
        });
        server.listen(0, "127.0.0.1").toCompletionStage().toCompletableFuture().join();

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Scope-OrgID", "tenant-1");
        headers.put("Authorization", "Bearer secret");
        client = new TempoHttpClient(buildHttpClient(server.actualPort()), headers);
    }

    @AfterEach
    void tear_down() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close().toCompletionStage().toCompletableFuture().join();
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    @Test
    void should_decode_a_successful_get_trace_response() {
        stubHandler =
            req ->
                req
                    .response()
                    .putHeader("Content-Type", "application/json")
                    .end(
                        "{\"batches\":[{\"resource\":{\"attributes\":[{\"key\":\"service.name\",\"value\":{\"stringValue\":\"gateway\"}}]}," +
                        "\"scopeSpans\":[{\"scope\":{\"name\":\"scope\"},\"spans\":[]}]}]}"
                    );

        TempoTraceResponse response = client.getTrace("abc-123").blockingGet();

        assertThat(response).isNotNull();
        assertThat(response.batches()).hasSize(1);
        assertThat(response.batches().get(0).resource().attributes())
            .singleElement()
            .satisfies(kv -> assertThat(kv.value().asString()).isEqualTo("gateway"));
    }

    @Test
    void should_send_static_headers_and_default_accept_on_each_request() {
        stubHandler = req -> req.response().end("{}");

        client.getTrace("abc-123").blockingGet();

        assertThat(recorded)
            .singleElement()
            .satisfies(r -> {
                assertThat(r.headers().get("Accept")).isEqualTo("application/json");
                assertThat(r.headers().get("X-Scope-OrgID")).isEqualTo("tenant-1");
                assertThat(r.headers().get("Authorization")).isEqualTo("Bearer secret");
            });
    }

    @Test
    void should_url_encode_the_traceql_query_and_path_segments() {
        stubHandler = req -> req.response().end("{\"traces\":[]}");

        String traceQL = "{ span.http.method = \"GET\" } | select(span.http.target)";
        client.searchTracesTraceQL(traceQL, 10, 1700000000L, 1700000060L).blockingGet();

        assertThat(recorded)
            .singleElement()
            .satisfies(r -> {
                assertThat(r.uri()).startsWith("/api/search?q=");
                assertThat(r.uri()).contains("limit=10");
                assertThat(r.uri()).contains("start=1700000000");
                assertThat(r.uri()).contains("end=1700000060");
                // The encoded query carries the raw TraceQL even though it contains spaces, quotes and braces.
                assertThat(
                    java.net.URLDecoder.decode(
                        r.uri().substring(r.uri().indexOf("q=") + 2, r.uri().indexOf("&")),
                        java.nio.charset.StandardCharsets.UTF_8
                    )
                )
                    .isEqualTo(traceQL);
            });
    }

    @Test
    void should_url_encode_the_trace_id() {
        stubHandler = req -> req.response().end("{\"batches\":[]}");

        client.getTrace("abc/with weird?chars").blockingGet();

        assertThat(recorded).singleElement().satisfies(r -> assertThat(r.uri()).isEqualTo("/api/traces/abc%2Fwith+weird%3Fchars"));
    }

    @Test
    void should_raise_tempo_client_exception_on_4xx() {
        stubHandler = req -> req.response().setStatusCode(404).end("not found");

        assertThatThrownBy(() -> client.getTrace("abc-123").blockingGet())
            .isInstanceOf(TempoClientException.class)
            .hasMessageContaining("404");
    }

    @Test
    void should_raise_tempo_client_exception_on_5xx() {
        stubHandler = req -> req.response().setStatusCode(500).end("internal error");

        assertThatThrownBy(() -> client.getTrace("abc-123").blockingGet())
            .isInstanceOf(TempoClientException.class)
            .hasMessageContaining("500");
    }

    private io.vertx.rxjava3.core.http.HttpClient buildHttpClient(int port) {
        return VertxHttpClientFactory
            .builder()
            .vertx(io.vertx.rxjava3.core.Vertx.newInstance(vertx))
            .nodeConfiguration(staticConfiguration())
            .defaultTarget("http://127.0.0.1:" + port)
            .name("tempo-http-client-test")
            .sslOptions(SslOptions.builder().build())
            .httpOptions(VertxHttpClientOptions.builder().connectTimeout(2000).idleTimeout(5000).build())
            .build()
            .createHttpClient();
    }

    private static Configuration staticConfiguration() {
        return new Configuration() {
            @Override
            public boolean containsProperty(String key) {
                return false;
            }

            @Override
            public String getProperty(String key) {
                return null;
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                return defaultValue;
            }

            @Override
            public <T> T getProperty(String key, Class<T> targetType) {
                return null;
            }

            @Override
            public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
                return defaultValue;
            }
        };
    }

    private static Map<String, List<String>> copyHeaders(HttpServerRequest req) {
        Map<String, List<String>> copy = new HashMap<>();
        req.headers().forEach(h -> copy.computeIfAbsent(h.getKey(), k -> new java.util.ArrayList<>()).add(h.getValue()));
        return copy;
    }

    private record RecordedRequest(String method, String uri, Map<String, List<String>> headerMultimap) {
        Map<String, String> headers() {
            Map<String, String> flat = new HashMap<>();
            headerMultimap.forEach((k, v) -> flat.put(k, v.get(0)));
            return flat;
        }
    }
}
