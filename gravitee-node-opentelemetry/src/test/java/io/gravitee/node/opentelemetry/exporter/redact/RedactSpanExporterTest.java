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
package io.gravitee.node.opentelemetry.exporter.redact;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import io.gravitee.node.api.opentelemetry.redaction.RedactionRule;
import io.gravitee.node.opentelemetry.tracer.instrumentation.internal.InternalInstrumenterTracer;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedactSpanExporterTest {

    private InMemorySpanExporter inMemoryExporter;

    @BeforeEach
    void setUp() {
        inMemoryExporter = InMemorySpanExporter.create();
    }

    @Test
    void should_redact_span_attribute_matching_rule(Vertx vertx) {
        var tracer = buildTracer(new RedactionConfig(List.of(new RedactionRule("custom"))));
        var vertxCtx = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        var span = tracer.startSpan(
            vertxCtx,
            InternalRequest.builder().name("test-span").attributes(Map.of("custom", "secret-value")).build(),
            false,
            null
        );
        tracer.endSpan(vertxCtx, span, null, null);

        List<SpanData> spans = inMemoryExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getAttributes().get(AttributeKey.stringKey("custom"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_not_redact_non_matching_span_attribute(Vertx vertx) {
        var tracer = buildTracer(new RedactionConfig(List.of(new RedactionRule("enduser.id"))));
        var vertxCtx = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        var span = tracer.startSpan(
            vertxCtx,
            InternalRequest.builder().name("test-span").attributes(Map.of("custom", "value")).build(),
            false,
            null
        );
        tracer.endSpan(vertxCtx, span, null, null);

        List<SpanData> spans = inMemoryExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getAttributes().get(AttributeKey.stringKey("custom"))).isEqualTo("value");
    }

    @Test
    void should_redact_attributes_matching_glob_pattern(Vertx vertx) {
        var tracer = buildTracer(new RedactionConfig(List.of(new RedactionRule("http.request.header.*"))));
        var vertxCtx = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        var request = InternalRequest
            .builder()
            .name("test-span")
            .attributes(
                Map.of("http.request.header.authorization", "Bearer token", "http.request.header.x-api-key", "my-key", "http.method", "GET")
            )
            .build();
        var span = tracer.startSpan(vertxCtx, request, false, null);
        tracer.endSpan(vertxCtx, span, null, null);

        List<SpanData> spans = inMemoryExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        var attrs = spans.get(0).getAttributes();
        assertThat(attrs.get(AttributeKey.stringKey("http.request.header.authorization"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(attrs.get(AttributeKey.stringKey("http.request.header.x-api-key"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(attrs.get(AttributeKey.stringKey("http.method"))).isEqualTo("GET");
    }

    @Test
    void should_delegate_flush() {
        try (var exporter = new RedactSpanExporter(inMemoryExporter, new RedactionConfig(List.of(new RedactionRule("anything"))))) {
            assertThat(exporter.flush().isSuccess()).isTrue();
        }
    }

    @Test
    void should_delegate_shutdown() {
        try (var exporter = new RedactSpanExporter(inMemoryExporter, new RedactionConfig(List.of(new RedactionRule("anything"))))) {
            assertThat(exporter.shutdown().isSuccess()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Export fast-path: original collection returned when no span changes
    // -------------------------------------------------------------------------

    @Nested
    class ExportCollectionIdentity {

        @Test
        void should_return_original_collection_reference_when_no_span_matches() {
            // Arrange: exporter with a rule that will not match any attribute
            AtomicReference<Collection<SpanData>> received = new AtomicReference<>();
            SpanExporter tracking = capturingExporter(received);
            var exporter = new RedactSpanExporter(tracking, new RedactionConfig(List.of(new RedactionRule("never.matches.key"))));

            var span = testSpan("http.method", "GET");
            var original = List.of(span);

            // Act
            exporter.export(original);

            // Assert: the exact same collection reference was forwarded — zero allocation
            assertThat(received.get()).isSameAs(original);
        }

        @Test
        void should_only_wrap_spans_that_need_redaction() {
            // Arrange: two spans — only the second has a matching attribute
            AtomicReference<Collection<SpanData>> received = new AtomicReference<>();
            SpanExporter tracking = capturingExporter(received);
            var exporter = new RedactSpanExporter(tracking, new RedactionConfig(List.of(new RedactionRule("secret.key"))));

            SpanData safe = testSpan("http.method", "GET");
            SpanData sensitive = testSpan("secret.key", "password");

            // Act
            exporter.export(List.of(safe, sensitive));

            // Assert: result list has two entries
            var resultList = new ArrayList<>(received.get());
            assertThat(resultList).hasSize(2);
            // First span was unchanged — original reference is reused
            assertThat(resultList.get(0)).isSameAs(safe);
            // Second span was wrapped — reference is NOT the original
            assertThat(resultList.get(1)).isNotSameAs(sensitive);
            assertThat(resultList.get(1).getAttributes().get(AttributeKey.stringKey("secret.key")))
                .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }
    }

    // -------------------------------------------------------------------------
    // Event attribute redaction through export()
    // -------------------------------------------------------------------------

    @Nested
    class EventRedaction {

        @Test
        void should_redact_event_attribute_matching_rule() {
            // Span has no matching span-level attribute; the matching key is on a span event.
            AtomicReference<Collection<SpanData>> received = new AtomicReference<>();
            var exporter = new RedactSpanExporter(
                capturingExporter(received),
                new RedactionConfig(List.of(new RedactionRule("secret.key")))
            );

            SpanData span = TestSpanData
                .builder()
                .setName("test")
                .setKind(SpanKind.INTERNAL)
                .setStartEpochNanos(0)
                .setEndEpochNanos(1)
                .setHasEnded(true)
                .setStatus(StatusData.ok())
                .setAttributes(Attributes.empty())
                .setEvents(
                    List.of(EventData.create(1L, "request-received", Attributes.of(AttributeKey.stringKey("secret.key"), "password"), 1))
                )
                .build();

            exporter.export(List.of(span));

            var result = new ArrayList<>(received.get());
            assertThat(result).hasSize(1);
            // Span was wrapped — not the original reference
            assertThat(result.get(0)).isNotSameAs(span);
            // Event attribute is redacted
            assertThat(result.get(0).getEvents().get(0).getAttributes().get(AttributeKey.stringKey("secret.key")))
                .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }

        @Test
        void should_return_original_collection_when_event_attributes_do_not_match() {
            AtomicReference<Collection<SpanData>> received = new AtomicReference<>();
            var exporter = new RedactSpanExporter(
                capturingExporter(received),
                new RedactionConfig(List.of(new RedactionRule("never.matches")))
            );

            SpanData span = TestSpanData
                .builder()
                .setName("test")
                .setKind(SpanKind.INTERNAL)
                .setStartEpochNanos(0)
                .setEndEpochNanos(1)
                .setHasEnded(true)
                .setStatus(StatusData.ok())
                .setAttributes(Attributes.empty())
                .setEvents(List.of(EventData.create(1L, "safe-event", Attributes.of(AttributeKey.stringKey("safe"), "value"), 1)))
                .build();

            var original = List.of(span);
            exporter.export(original);

            // No match — original collection reference forwarded unchanged
            assertThat(received.get()).isSameAs(original);
        }

        @Test
        void should_preserve_unchanged_events_by_reference_when_only_middle_event_matches() {
            // Events: [safe, sensitive, safe] — only the second changes.
            // Verifies the lazy-backfill logic in redactEvents(): first and third events
            // must be the exact same EventData instances as the originals.
            AtomicReference<Collection<SpanData>> received = new AtomicReference<>();
            var exporter = new RedactSpanExporter(
                capturingExporter(received),
                new RedactionConfig(List.of(new RedactionRule("secret.key")))
            );

            EventData safeEvent1 = EventData.create(1L, "evt1", Attributes.of(AttributeKey.stringKey("safe"), "a"), 1);
            EventData sensitiveEvent = EventData.create(2L, "evt2", Attributes.of(AttributeKey.stringKey("secret.key"), "password"), 1);
            EventData safeEvent2 = EventData.create(3L, "evt3", Attributes.of(AttributeKey.stringKey("safe"), "b"), 1);

            SpanData span = TestSpanData
                .builder()
                .setName("test")
                .setKind(SpanKind.INTERNAL)
                .setStartEpochNanos(0)
                .setEndEpochNanos(1)
                .setHasEnded(true)
                .setStatus(StatusData.ok())
                .setAttributes(Attributes.empty())
                .setEvents(List.of(safeEvent1, sensitiveEvent, safeEvent2))
                .build();

            exporter.export(List.of(span));

            var events = new ArrayList<>(received.get()).get(0).getEvents();
            assertThat(events).hasSize(3);
            // First and third are unchanged — same references
            assertThat(events.get(0)).isSameAs(safeEvent1);
            assertThat(events.get(2)).isSameAs(safeEvent2);
            // Second is a new EventData with the redacted value
            assertThat(events.get(1)).isNotSameAs(sensitiveEvent);
            assertThat(events.get(1).getAttributes().get(AttributeKey.stringKey("secret.key")))
                .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }
    }

    // -------------------------------------------------------------------------
    // redactResource instance method
    // -------------------------------------------------------------------------

    @Nested
    class RedactResourceMethod {

        @Test
        void should_redact_resource_attribute_matching_rule() {
            var exporter = new RedactSpanExporter(inMemoryExporter, new RedactionConfig(List.of(new RedactionRule("hostname"))));
            var resource = Resource.create(Attributes.of(AttributeKey.stringKey("hostname"), "prod-host-01"));

            var result = exporter.redactResource(resource);

            assertThat(result.getAttributes().get(AttributeKey.stringKey("hostname"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }

        @Test
        void should_return_same_resource_reference_when_no_rule_matches() {
            var exporter = new RedactSpanExporter(inMemoryExporter, new RedactionConfig(List.of(new RedactionRule("never.matches"))));
            var resource = Resource.create(Attributes.of(AttributeKey.stringKey("hostname"), "prod-host-01"));

            assertThat(exporter.redactResource(resource)).isSameAs(resource);
        }

        @Test
        void should_preserve_schema_url_when_resource_attribute_is_redacted() {
            var exporter = new RedactSpanExporter(inMemoryExporter, new RedactionConfig(List.of(new RedactionRule("hostname"))));
            var schemaUrl = "https://opentelemetry.io/schemas/1.21.0";
            var resource = Resource.create(Attributes.of(AttributeKey.stringKey("hostname"), "prod-host-01"), schemaUrl);

            var result = exporter.redactResource(resource);

            assertThat(result.getSchemaUrl()).isEqualTo(schemaUrl);
            assertThat(result.getAttributes().get(AttributeKey.stringKey("hostname"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link SpanExporter} that captures every collection passed to {@link SpanExporter#export}
     * into {@code sink}. SpanExporter has three abstract methods so it cannot be a lambda.
     */
    private static SpanExporter capturingExporter(AtomicReference<Collection<SpanData>> sink) {
        return new SpanExporter() {
            @Override
            public io.opentelemetry.sdk.common.CompletableResultCode export(Collection<SpanData> spans) {
                sink.set(spans);
                return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();
            }

            @Override
            public io.opentelemetry.sdk.common.CompletableResultCode flush() {
                return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();
            }

            @Override
            public io.opentelemetry.sdk.common.CompletableResultCode shutdown() {
                return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();
            }
        };
    }

    /** Builds a minimal {@link SpanData} with a single string attribute for unit tests. */
    private static SpanData testSpan(String attrKey, String attrValue) {
        return TestSpanData
            .builder()
            .setName("test")
            .setKind(SpanKind.INTERNAL)
            .setStartEpochNanos(0)
            .setEndEpochNanos(1)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setAttributes(Attributes.of(AttributeKey.stringKey(attrKey), attrValue))
            .build();
    }

    private InternalInstrumenterTracer buildTracer(RedactionConfig config) {
        var redactExporter = new RedactSpanExporter(inMemoryExporter, config);
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(redactExporter)).build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
        return new InternalInstrumenterTracer(openTelemetry);
    }
}
