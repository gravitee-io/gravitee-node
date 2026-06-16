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
package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpClientRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpClientResponse;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpServerRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpServerResponse;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContextStorage;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxHttpInstrumenterTracerTest {

    private InMemorySpanExporter spanExporter;
    private VertxHttpInstrumenterTracer tracer;
    private OpenTelemetrySdk openTelemetry;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spanExporter)).build();
        openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
        tracer = new VertxHttpInstrumenterTracer(openTelemetry);
    }

    @Test
    void should_start_root_span_with_server_kind_when_vertx_context_has_non_root_otel_context(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelTracer = openTelemetry.getTracer("test");
        var existingSpan = otelTracer.spanBuilder("existing").setSpanKind(SpanKind.SERVER).startSpan();

        try (var ignored = VertxContextStorage.INSTANCE.attach(vertxContext, Context.root().with(existingSpan))) {
            HttpServerRequest serverRequest = mockServerRequest(HttpMethod.POST, "/test");
            HttpServerResponse serverResponse = mockServerResponse(200);

            Span rootSpan = tracer.startSpan(vertxContext, new ObservableHttpServerRequest(serverRequest), true, null);
            tracer.endSpan(vertxContext, rootSpan, new ObservableHttpServerResponse(serverResponse), null);
        } finally {
            existingSpan.end();
        }

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        SpanData serverSpan = spans.stream().filter(span -> span.getKind() == SpanKind.SERVER).findFirst().orElseThrow();
        assertThat(serverSpan.getName()).isEqualTo("POST /test");
        assertThat(serverSpan.getParentSpanContext().isValid()).isFalse();
    }

    @Test
    void should_keep_server_span_name_when_client_span_ends(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        HttpServerRequest serverRequest = mockServerRequest(HttpMethod.POST, "/test");
        HttpServerResponse serverResponse = mockServerResponse(200);

        Span rootSpan = tracer.startSpan(vertxContext, new ObservableHttpServerRequest(serverRequest), true, null);

        RequestOptions clientOptions = new RequestOptions()
            .setMethod(HttpMethod.POST)
            .setURI("/endpoint")
            .setHost("localhost")
            .setPort(8080)
            .setServer(SocketAddress.inetSocketAddress(8080, "localhost"));
        Span clientSpan = tracer.startSpan(vertxContext, new ObservableHttpClientRequest(clientOptions), false, rootSpan);

        HttpClientResponse clientResponse = mock(HttpClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(200);
        when(clientResponse.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());

        tracer.endSpan(vertxContext, clientSpan, new ObservableHttpClientResponse(clientResponse), null);
        tracer.endSpan(vertxContext, rootSpan, new ObservableHttpServerResponse(serverResponse), null);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(2);

        SpanData serverSpan = spans.stream().filter(span -> span.getKind() == SpanKind.SERVER).findFirst().orElseThrow();
        SpanData backendSpan = spans.stream().filter(span -> span.getKind() == SpanKind.CLIENT).findFirst().orElseThrow();

        assertThat(serverSpan.getName()).isEqualTo("POST /test");
        assertThat(backendSpan.getName()).isNotEqualTo("POST /test");
        assertThat(backendSpan.getParentSpanId()).isEqualTo(serverSpan.getSpanId());
    }

    private static HttpServerRequest mockServerRequest(HttpMethod method, String uri) {
        HttpServerRequest serverRequest = mock(HttpServerRequest.class);
        when(serverRequest.method()).thenReturn(method);
        when(serverRequest.uri()).thenReturn(uri);
        when(serverRequest.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(serverRequest.scheme()).thenReturn("http");
        when(serverRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(12345, "127.0.0.1"));
        return serverRequest;
    }

    private static HttpServerResponse mockServerResponse(int statusCode) {
        HttpServerResponse serverResponse = mock(HttpServerResponse.class);
        when(serverResponse.getStatusCode()).thenReturn(statusCode);
        when(serverResponse.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        return serverResponse;
    }
}
