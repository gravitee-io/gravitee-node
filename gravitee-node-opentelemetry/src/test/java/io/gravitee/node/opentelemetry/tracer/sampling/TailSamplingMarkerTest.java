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
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TailSamplingMarkerTest {

    private static final AttributeKey<Boolean> TAIL = AttributeKey.booleanKey(SamplingConfig.ATTR_TAIL);

    private final AtomicReference<SamplingConfig> config = new AtomicReference<>(SamplingConfig.ALWAYS);
    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
        .addSpanProcessor(new TailSamplingMarker(config::get))
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build();
    private final Tracer tracer = provider.get("test");

    @AfterEach
    void tearDown() {
        provider.close();
    }

    @Test
    void should_mark_every_span_of_the_trace_when_tail_criteria_are_set() {
        // Given
        config.set(new SamplingConfig(1.0, true, Set.of(), List.of(), Set.of(405), false));

        // When
        trace();

        // Then
        assertThat(exporter.getFinishedSpanItems())
            .hasSize(2)
            .allMatch(span -> Boolean.TRUE.equals(span.getAttributes().get(TAIL)));
    }

    @Test
    void should_not_mark_spans_without_tail_criteria() {
        // When
        trace();

        // Then
        assertThat(exporter.getFinishedSpanItems())
            .hasSize(2)
            .allMatch(span -> span.getAttributes().get(TAIL) == null);
    }

    @Test
    void should_keep_a_listed_status_or_a_failure_only() {
        // Given
        var criteria = new SamplingConfig(1.0, true, Set.of(), List.of(), Set.of(405), true);

        // Then
        assertThat(criteria.keeps(405, false)).isTrue();
        assertThat(criteria.keeps(502, false)).isTrue();
        assertThat(criteria.keeps(200, true)).isTrue();
        assertThat(criteria.keeps(200, false)).isFalse();
        assertThat(criteria.keeps(403, false)).isFalse();
    }

    private void trace() {
        final Span root = tracer.spanBuilder("GET /orders").startSpan();
        tracer.spanBuilder("policy").setParent(Context.root().with(root)).startSpan().end();
        root.end();
    }
}
