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
package io.gravitee.node.opentelemetry.tracer.instrumentation.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InternalInstrumenterTracerTest {

    private InMemorySpanExporter spanExporter;
    private InternalInstrumenterTracer tracer;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spanExporter)).build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
        tracer = new InternalInstrumenterTracer(openTelemetry);
    }

    @Test
    void should_create_span_with_internal_kind_by_default(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var request = InternalRequest.builder().name("my-span").attributes(Map.of("custom", "value")).build();

        Span span = tracer.startSpan(vertxContext, request, false, null);
        tracer.endSpan(vertxContext, span, null, null);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getName()).isEqualTo("my-span");
        assertThat(spans.get(0).getKind()).isEqualTo(SpanKind.INTERNAL);
    }

    @Test
    void should_create_span_with_client_kind_when_specified(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var request = InternalRequest.builder().name("my-client-span").spanKind(SpanKind.CLIENT).build();

        Span span = tracer.startSpan(vertxContext, request, false, null);
        tracer.endSpan(vertxContext, span, null, null);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getName()).isEqualTo("my-client-span");
        assertThat(spans.get(0).getKind()).isEqualTo(SpanKind.CLIENT);
    }

    @Test
    void should_create_span_with_producer_kind_when_specified(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var request = InternalRequest.builder().name("my-producer-span").spanKind(SpanKind.PRODUCER).build();

        Span span = tracer.startSpan(vertxContext, request, false, null);
        tracer.endSpan(vertxContext, span, null, null);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getName()).isEqualTo("my-producer-span");
        assertThat(spans.get(0).getKind()).isEqualTo(SpanKind.PRODUCER);
    }
}
