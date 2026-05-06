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
package io.gravitee.node.opentelemetry.tracer.span;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.opentelemetry.tracer.noop.NoOpSpan;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenTelemetrySpanIdTest {

    private OpenTelemetrySdk openTelemetry;

    @BeforeEach
    void setUp() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spanExporter)).build();
        openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    }

    @Test
    void traceId_should_match_underlying_otel_span_context(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelSpan = openTelemetry.getTracer("test").spanBuilder("test-span").startSpan();
        var otelContext = Context.root().with(otelSpan);
        var span = new OpenTelemetrySpan<>(vertxContext, otelContext, Scope.noop(), false, "request");

        assertThat(span.traceId()).hasSize(32);
        assertThat(span.traceId()).isEqualTo(otelSpan.getSpanContext().getTraceId());
        otelSpan.end();
    }

    @Test
    void spanId_should_match_underlying_otel_span_context(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelSpan = openTelemetry.getTracer("test").spanBuilder("test-span").startSpan();
        var otelContext = Context.root().with(otelSpan);
        var span = new OpenTelemetrySpan<>(vertxContext, otelContext, Scope.noop(), false, "request");

        assertThat(span.spanId()).hasSize(16);
        assertThat(span.spanId()).isEqualTo(otelSpan.getSpanContext().getSpanId());
        otelSpan.end();
    }

    @Test
    void traceId_and_spanId_should_remain_valid_after_otel_span_ended(Vertx vertx) {
        // Span IDs come from SpanContext, which is immutable — ending the span does not invalidate
        // the IDs. This is the property that makes Span#spanId() reliable in non-LIFO flows where
        // the per-context slot has already been restored to null by an out-of-order end.
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelSpan = openTelemetry.getTracer("test").spanBuilder("test-span").startSpan();
        var otelContext = Context.root().with(otelSpan);
        var span = new OpenTelemetrySpan<>(vertxContext, otelContext, Scope.noop(), false, "request");
        var expectedTraceId = otelSpan.getSpanContext().getTraceId();
        var expectedSpanId = otelSpan.getSpanContext().getSpanId();
        otelSpan.end();

        assertThat(span.traceId()).isEqualTo(expectedTraceId);
        assertThat(span.spanId()).isEqualTo(expectedSpanId);
    }

    @Test
    void sibling_spans_should_each_return_their_own_ids(Vertx vertx) {
        // Two spans sharing the same Vert.x context still report their own IDs, regardless of
        // which one is currently attached to the context slot.
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelTracer = openTelemetry.getTracer("test");

        var otelSpanA = otelTracer.spanBuilder("A").startSpan();
        var otelSpanB = otelTracer.spanBuilder("B").startSpan();
        var spanA = new OpenTelemetrySpan<>(vertxContext, Context.root().with(otelSpanA), Scope.noop(), false, "A");
        var spanB = new OpenTelemetrySpan<>(vertxContext, Context.root().with(otelSpanB), Scope.noop(), false, "B");

        assertThat(spanA.spanId()).isEqualTo(otelSpanA.getSpanContext().getSpanId());
        assertThat(spanB.spanId()).isEqualTo(otelSpanB.getSpanContext().getSpanId());
        assertThat(spanA.spanId()).isNotEqualTo(spanB.spanId());

        otelSpanA.end();
        otelSpanB.end();
    }

    @Test
    void noOpSpan_traceId_should_return_empty() {
        assertThat(NoOpSpan.asRoot().traceId()).isEmpty();
        assertThat(NoOpSpan.asDefault().traceId()).isEmpty();
    }

    @Test
    void noOpSpan_spanId_should_return_empty() {
        assertThat(NoOpSpan.asRoot().spanId()).isEmpty();
        assertThat(NoOpSpan.asDefault().spanId()).isEmpty();
    }
}
