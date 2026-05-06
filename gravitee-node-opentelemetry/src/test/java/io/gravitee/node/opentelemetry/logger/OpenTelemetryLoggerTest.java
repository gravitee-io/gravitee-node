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
package io.gravitee.node.opentelemetry.logger;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.opentelemetry.logger.noop.NoOpLogger;
import io.gravitee.node.opentelemetry.tracer.span.OpenTelemetrySpan;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContext;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContextStorage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenTelemetryLoggerTest {

    private InMemoryLogRecordExporter logExporter;
    private OpenTelemetrySdk openTelemetry;
    private OpenTelemetryLogger logger;

    @BeforeEach
    void setUp() throws Exception {
        logExporter = InMemoryLogRecordExporter.create();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider
            .builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
            .build();
        openTelemetry = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build();
        logger = new OpenTelemetryLogger(openTelemetry);
        logger.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        logger.stop();
    }

    @Test
    void record_should_attach_log_to_explicit_span_context(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelSpan = openTelemetry.getTracer("test").spanBuilder("test-span").startSpan();
        Span span = new OpenTelemetrySpan<>(vertxContext, Context.root().with(otelSpan), Scope.noop(), false, "request");

        logger.record(vertxContext, span, "hello", Map.of("key", "value"));

        List<LogRecordData> records = logExporter.getFinishedLogRecordItems();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBody().asString()).isEqualTo("hello");
        assertThat(records.get(0).getSpanContext().getTraceId()).isEqualTo(otelSpan.getSpanContext().getTraceId());
        assertThat(records.get(0).getSpanContext().getSpanId()).isEqualTo(otelSpan.getSpanContext().getSpanId());
        otelSpan.end();
    }

    @Test
    void record_should_correlate_log_with_span_even_when_slot_is_empty(Vertx vertx) {
        // The motivating case: in multiplexed flows the per-context slot may not reflect the span
        // the caller cares about. Passing the span explicitly bypasses the slot lookup.
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelSpan = openTelemetry.getTracer("test").spanBuilder("test-span").startSpan();
        Span span = new OpenTelemetrySpan<>(vertxContext, Context.root().with(otelSpan), Scope.noop(), false, "request");
        // Slot is intentionally never attached.
        assertThat(VertxContextStorage.getContext(vertxContext)).isNull();

        logger.record(vertxContext, span, "hello", Map.of());

        List<LogRecordData> records = logExporter.getFinishedLogRecordItems();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getSpanContext().getSpanId()).isEqualTo(otelSpan.getSpanContext().getSpanId());
        otelSpan.end();
    }

    @Test
    void record_with_null_span_should_fall_back_to_vertx_context_slot(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        var otelSpan = openTelemetry.getTracer("test").spanBuilder("test-span").startSpan();

        try (var ignored = VertxContextStorage.INSTANCE.attach(vertxContext, Context.root().with(otelSpan))) {
            logger.record(vertxContext, null, "hello", Map.of());
        }

        List<LogRecordData> records = logExporter.getFinishedLogRecordItems();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getSpanContext().getSpanId()).isEqualTo(otelSpan.getSpanContext().getSpanId());
        otelSpan.end();
    }

    @Test
    void record_with_null_span_and_empty_slot_should_emit_uncorrelated_record(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "hello", Map.of());

        List<LogRecordData> records = logExporter.getFinishedLogRecordItems();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBody().asString()).isEqualTo("hello");
        assertThat(records.get(0).getSpanContext().isValid()).isFalse();
    }

    @Test
    void record_should_propagate_string_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "body", Map.of("k", "v"));

        var attrs = logExporter.getFinishedLogRecordItems().get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.stringKey("k"))).isEqualTo("v");
    }

    @Test
    void record_should_propagate_long_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "body", Map.of("k", 42L));

        var attrs = logExporter.getFinishedLogRecordItems().get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.longKey("k"))).isEqualTo(42L);
    }

    @Test
    void record_should_propagate_integer_as_long_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "body", Map.of("k", 7));

        var attrs = logExporter.getFinishedLogRecordItems().get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.longKey("k"))).isEqualTo(7L);
    }

    @Test
    void record_should_propagate_double_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "body", Map.of("k", 1.5d));

        var attrs = logExporter.getFinishedLogRecordItems().get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.doubleKey("k"))).isEqualTo(1.5d);
    }

    @Test
    void record_should_propagate_boolean_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "body", Map.of("k", true));

        var attrs = logExporter.getFinishedLogRecordItems().get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.booleanKey("k"))).isTrue();
    }

    @Test
    void record_should_stringify_unsupported_attribute_types(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "body", Map.of("k", List.of("a", "b")));

        var attrs = logExporter.getFinishedLogRecordItems().get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.stringKey("k"))).isEqualTo(List.of("a", "b").toString());
    }

    @Test
    void record_should_skip_null_attribute_values(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("present", "value");
        attributes.put("absent", null);

        logger.record(vertxContext, null, "body", attributes);

        var attrs = logExporter.getFinishedLogRecordItems().get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.stringKey("present"))).isEqualTo("value");
        assertThat(attrs.asMap()).doesNotContainKey(AttributeKey.stringKey("absent"));
    }

    @Test
    void record_with_null_attributes_map_should_emit_record_with_no_attributes(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, null, "body", null);

        var record = logExporter.getFinishedLogRecordItems().get(0);
        assertThat(record.getBody().asString()).isEqualTo("body");
        assertThat(record.getAttributes().isEmpty()).isTrue();
    }

    @Test
    void record_two_arg_overload_should_emit_record_with_empty_attributes(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        logger.record(vertxContext, "body");

        var record = logExporter.getFinishedLogRecordItems().get(0);
        assertThat(record.getBody().asString()).isEqualTo("body");
        assertThat(record.getAttributes().isEmpty()).isTrue();
    }

    @Test
    void noOpLogger_record_should_not_throw(Vertx vertx) {
        var noOp = new NoOpLogger();
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        noOp.record(vertxContext, "body");
        noOp.record(vertxContext, "body", Map.of("k", "v"));
        noOp.record(vertxContext, null, "body", Map.of("k", "v"));
    }
}
