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
package io.gravitee.node.opentelemetry.tracer.sampling;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.opentelemetry.sampling.SamplingConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteeSamplerTest {

    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");
    private static final String REMOTE_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String REMOTE_SPAN_ID = "00f067aa0ba902b7";

    private final AtomicReference<SamplingConfig> config = new AtomicReference<>(SamplingConfig.ALWAYS);
    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
        .setSampler(new GraviteeSampler(config::get))
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build();
    private final Tracer tracer = provider.get("test");

    @AfterEach
    void tearDown() {
        provider.close();
    }

    @Nested
    class On_a_root_span {

        @Test
        void should_record_every_request_by_default() {
            // When
            request("GET", "/orders", Context.root());

            // Then
            assertThat(exporter.getFinishedSpanItems()).hasSize(2);
        }

        @Test
        void should_drop_the_whole_trace_of_a_method_not_listed() {
            // Given
            config.set(new SamplingConfig(1.0, true, Set.of("GET"), List.of(), Set.of(), false));

            // When
            request("POST", "/orders", Context.root());
            request("GET", "/orders", Context.root());

            // Then
            assertThat(exporter.getFinishedSpanItems())
                .extracting(span -> span.getAttributes().get(HTTP_METHOD))
                .containsOnly("GET", null);
            assertThat(exporter.getFinishedSpanItems()).hasSize(2);
        }

        @Test
        void should_drop_a_path_outside_the_prefixes() {
            // Given
            config.set(new SamplingConfig(1.0, true, Set.of(), List.of("/orders"), Set.of(), false));

            // When
            request("GET", "/health", Context.root());

            // Then
            assertThat(exporter.getFinishedSpanItems()).isEmpty();
        }

        @Test
        void should_record_nothing_with_a_zero_ratio() {
            // Given
            config.set(new SamplingConfig(0.0, true, Set.of(), List.of(), Set.of(), false));

            // When
            for (int i = 0; i < 50; i++) {
                request("GET", "/orders", Context.root());
            }

            // Then
            assertThat(exporter.getFinishedSpanItems()).isEmpty();
        }

        @Test
        void should_apply_a_config_changed_after_the_tracer_was_created() {
            // Given
            request("GET", "/orders", Context.root());

            // When
            config.set(new SamplingConfig(0.0, true, Set.of(), List.of(), Set.of(), false));
            request("GET", "/orders", Context.root());

            // Then
            assertThat(exporter.getFinishedSpanItems()).hasSize(2);
        }
    }

    @Nested
    class With_an_incoming_traceparent {

        @Test
        void should_follow_an_unsampled_caller_when_respecting_remote_parents() {
            // When
            request("GET", "/orders", remoteParent(TraceFlags.getDefault()));

            // Then
            assertThat(exporter.getFinishedSpanItems()).isEmpty();
        }

        @Test
        void should_decide_alone_when_remote_parents_are_ignored() {
            // Given
            config.set(new SamplingConfig(1.0, false, Set.of(), List.of(), Set.of(), false));

            // When
            request("GET", "/orders", remoteParent(TraceFlags.getDefault()));

            // Then
            assertThat(exporter.getFinishedSpanItems())
                .hasSize(2)
                .allMatch(span -> span.getTraceId().equals(REMOTE_TRACE_ID));
        }

        @Test
        void should_not_let_a_sampled_caller_force_a_dropped_method_when_remote_parents_are_ignored() {
            // Given
            config.set(new SamplingConfig(1.0, false, Set.of("GET"), List.of(), Set.of(), false));

            // When
            request("POST", "/orders", remoteParent(TraceFlags.getSampled()));

            // Then
            assertThat(exporter.getFinishedSpanItems()).isEmpty();
        }
    }

    private void request(final String method, final String path, final Context parent) {
        final Span root = tracer
            .spanBuilder(method + " " + path)
            .setParent(parent)
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(HTTP_METHOD, method)
            .setAttribute(HTTP_TARGET, path)
            .startSpan();
        final Span child = tracer.spanBuilder("policy").setParent(Context.root().with(root)).startSpan();
        child.end();
        root.end();
    }

    private static Context remoteParent(final TraceFlags flags) {
        return Context.root().with(
            Span.wrap(SpanContext.createFromRemoteParent(REMOTE_TRACE_ID, REMOTE_SPAN_ID, flags, TraceState.getDefault()))
        );
    }
}
