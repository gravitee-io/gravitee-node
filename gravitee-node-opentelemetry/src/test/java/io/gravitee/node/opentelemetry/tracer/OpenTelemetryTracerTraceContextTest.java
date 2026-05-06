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
package io.gravitee.node.opentelemetry.tracer;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContext;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContextStorage;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenTelemetryTracerTraceContextTest {

    private OpenTelemetryTracer tracer;
    private OpenTelemetrySdk openTelemetry;

    @BeforeEach
    void setUp() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spanExporter)).build();
        openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
        tracer = new OpenTelemetryTracer(openTelemetry, List.of());
    }

    @Test
    void traceId_should_return_empty_when_no_span_in_context(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        assertThat(tracer.traceId(vertxContext)).isEmpty();
    }

    @Test
    void spanId_should_return_empty_when_no_span_in_context(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        assertThat(tracer.spanId(vertxContext)).isEmpty();
    }

    @Test
    void traceId_should_return_valid_32_char_hex_when_span_is_active(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        var otelTracer = openTelemetry.getTracer("test");
        var span = otelTracer.spanBuilder("test-span").startSpan();
        Context otelContextWithSpan = Context.root().with(span);

        try (var ignored = VertxContextStorage.INSTANCE.attach(vertxContext, otelContextWithSpan)) {
            assertThat(tracer.traceId(vertxContext)).hasSize(32);
            assertThat(tracer.traceId(vertxContext)).isEqualTo(span.getSpanContext().getTraceId());
        } finally {
            span.end();
        }
    }

    @Test
    void spanId_should_return_valid_16_char_hex_when_span_is_active(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        var otelTracer = openTelemetry.getTracer("test");
        var span = otelTracer.spanBuilder("test-span").startSpan();
        Context otelContextWithSpan = Context.root().with(span);

        try (var ignored = VertxContextStorage.INSTANCE.attach(vertxContext, otelContextWithSpan)) {
            assertThat(tracer.spanId(vertxContext)).hasSize(16);
            assertThat(tracer.spanId(vertxContext)).isEqualTo(span.getSpanContext().getSpanId());
        } finally {
            span.end();
        }
    }

    @Test
    void noOpTracer_traceId_should_always_return_empty(Vertx vertx) {
        var noOp = new NoOpTracer();
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        assertThat(noOp.traceId(vertxContext)).isEmpty();
    }

    @Test
    void noOpTracer_spanId_should_always_return_empty(Vertx vertx) {
        var noOp = new NoOpTracer();
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        assertThat(noOp.spanId(vertxContext)).isEmpty();
    }

    @Test
    void spanId_is_unreliable_when_sibling_spans_close_out_of_order_on_same_context(Vertx vertx) {
        // Documents the caveat called out in Tracer#spanId(Context) javadoc: the per-context slot
        // behaves like a stack and does not survive non-LIFO close ordering. Asserts that the
        // Span-level accessor remains correct in the same scenario.
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelTracer = openTelemetry.getTracer("test");

        var spanA = otelTracer.spanBuilder("A").startSpan();
        var spanB = otelTracer.spanBuilder("B").startSpan();
        var scopeA = VertxContextStorage.INSTANCE.attach(vertxContext, Context.root().with(spanA));
        var scopeB = VertxContextStorage.INSTANCE.attach(vertxContext, Context.root().with(spanB));

        // Slot currently holds B (most recently attached) — Tracer#spanId reflects that.
        assertThat(tracer.spanId(vertxContext)).isEqualTo(spanB.getSpanContext().getSpanId());

        // Closing A's scope out of order restores the slot to A's pre-attach snapshot (null),
        // even though B is logically still in flight. Tracer#spanId now returns "" — the bug.
        scopeA.close();
        assertThat(tracer.spanId(vertxContext)).isEmpty();

        // The SDK span objects themselves still carry their IDs regardless of the slot state.
        // This is what consumers should rely on when spans are not strictly LIFO-nested.
        assertThat(spanA.getSpanContext().getSpanId()).hasSize(16);
        assertThat(spanB.getSpanContext().getSpanId()).hasSize(16);
        assertThat(spanA.getSpanContext().getSpanId()).isNotEqualTo(spanB.getSpanContext().getSpanId());

        scopeB.close();
        spanA.end();
        spanB.end();
    }
}
