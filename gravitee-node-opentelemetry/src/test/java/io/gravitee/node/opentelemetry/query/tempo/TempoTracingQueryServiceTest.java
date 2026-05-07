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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.opentelemetry.query.model.Trace;
import io.gravitee.node.api.opentelemetry.query.model.TraceSearchCriteria;
import io.gravitee.node.api.opentelemetry.query.model.TraceSpan;
import io.gravitee.node.api.opentelemetry.query.model.TracingQueryContext;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TempoTracingQueryServiceTest {

    @Mock
    TempoHttpClient tempoClient;

    @InjectMocks
    TempoTracingQueryService underTest;

    @Test
    void should_emit_empty_list_when_tempo_returns_no_traces() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any())).thenReturn(Single.just(new TempoSearchResponse(null)));

        List<Trace> traces = underTest
            .searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(Map.of(), 20, null, null))
            .blockingGet();

        assertThat(traces).isEmpty();
    }

    @Test
    void should_route_resource_attributes_under_resource_prefix() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        Map<String, String> attributeFilters = new LinkedHashMap<>();
        attributeFilters.put("service.name", "gateway");
        attributeFilters.put("http.method", "GET");
        underTest.searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(attributeFilters, 20, null, null)).blockingGet();

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(queryCaptor.capture(), any(), any(), any(), any());
        String primaryQuery = queryCaptor.getAllValues().get(0);
        assertThat(primaryQuery).contains("resource.service.name = \"gateway\"");
        assertThat(primaryQuery).contains("span.http.method = \"GET\"");
    }

    @Test
    void should_run_both_primary_and_error_passes() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        underTest.searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(Map.of(), 20, null, null)).blockingGet();

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(queryCaptor.capture(), any(), any(), any(), any());
        assertThat(queryCaptor.getAllValues().get(0)).doesNotContain("status = error");
        assertThat(queryCaptor.getAllValues().get(1)).contains("status = error");
    }

    @Test
    void should_flag_traces_returned_by_the_error_pass() {
        TempoSearchResponse primary = new TempoSearchResponse(List.of(traceResult("aaa"), traceResult("bbb")));
        TempoSearchResponse errors = new TempoSearchResponse(List.of(traceResult("bbb")));
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(primary))
            .thenReturn(Single.just(errors));

        List<Trace> traces = underTest
            .searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(Map.of(), 20, null, null))
            .blockingGet();

        assertThat(traces).hasSize(2);
        assertThat(traces.get(0).hasError()).isFalse();
        assertThat(traces.get(1).hasError()).isTrue();
    }

    @Test
    void should_treat_traces_as_unflagged_when_error_pass_fails() {
        // Older Tempo builds reject `status = error`; the list must still load with hasError=false.
        TempoSearchResponse primary = new TempoSearchResponse(List.of(traceResult("aaa")));
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(primary))
            .thenReturn(Single.error(new RuntimeException("intrinsic not supported")));

        List<Trace> traces = underTest
            .searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(Map.of(), 20, null, null))
            .blockingGet();

        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).hasError()).isFalse();
    }

    @Test
    void should_default_start_to_seven_days_before_end_when_only_end_is_set() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        Instant end = Instant.parse("2026-04-29T12:00:00Z");
        underTest.searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(Map.of(), 20, null, end)).blockingGet();

        ArgumentCaptor<Long> startCaptor = ArgumentCaptor.forClass(Long.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(any(), any(), startCaptor.capture(), eq(end.getEpochSecond()), any());
        long expectedStart = end.getEpochSecond() - 7L * 24 * 3600;
        assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
    }

    @Test
    void should_pass_root_trace_name_through_as_root_operation() {
        TempoSearchResponse.TraceResult result = new TempoSearchResponse.TraceResult(
            "abc",
            "gateway",
            "GET /proxy",
            Instant.parse("2026-04-29T12:00:00Z").toEpochMilli() * 1_000_000L,
            42L
        );
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of(result))));

        List<Trace> traces = underTest
            .searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(Map.of(), 20, null, null))
            .blockingGet();

        assertThat(traces)
            .singleElement()
            .satisfies(t -> {
                assertThat(t.rootService()).isEqualTo("gateway");
                assertThat(t.rootOperation()).isEqualTo("GET /proxy");
            });
    }

    @Test
    void should_return_empty_when_response_has_no_batches() {
        when(tempoClient.getTrace(eq("missing"), any())).thenReturn(Single.just(new TempoTraceResponse(null)));

        Optional<Trace> result = underTest.getTrace(TracingQueryContext.EMPTY, "missing").blockingGet() == null
            ? Optional.empty()
            : Optional.of(underTest.getTrace(TracingQueryContext.EMPTY, "missing").blockingGet());

        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_when_response_is_null() {
        when(tempoClient.getTrace(eq("missing"), any())).thenReturn(Single.just(new TempoTraceResponse(null)));

        assertThat(underTest.getTrace(TracingQueryContext.EMPTY, "missing").blockingGet()).isNull();
    }

    @Test
    void should_normalise_numeric_status_codes() {
        TempoTraceResponse response = singleBatch(
            "gateway",
            spanWithStatus("trace", "span-1", null, "GET", "0"),
            spanWithStatus("trace", "span-2", "span-1", "child", "1"),
            spanWithStatus("trace", "span-3", "span-1", "boom", "2")
        );
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(response));

        Trace trace = underTest.getTrace(TracingQueryContext.EMPTY, "trace").blockingGet();

        assertThat(trace).isNotNull();
        assertThat(trace.spans()).hasSize(3);
        assertThat(statusOf(trace, "span-1")).isEqualTo("UNSET");
        assertThat(statusOf(trace, "span-2")).isEqualTo("OK");
        assertThat(statusOf(trace, "span-3")).isEqualTo("ERROR");
        assertThat(trace.hasError()).isTrue();
    }

    @Test
    void should_normalise_proto_enum_status_codes() {
        TempoTraceResponse response = singleBatch(
            "gateway",
            spanWithStatus("trace", "span-1", null, "GET", "STATUS_CODE_UNSET"),
            spanWithStatus("trace", "span-2", "span-1", "child", "STATUS_CODE_OK"),
            spanWithStatus("trace", "span-3", "span-1", "boom", "STATUS_CODE_ERROR")
        );
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(response));

        Trace trace = underTest.getTrace(TracingQueryContext.EMPTY, "trace").blockingGet();

        assertThat(statusOf(trace, "span-1")).isEqualTo("UNSET");
        assertThat(statusOf(trace, "span-2")).isEqualTo("OK");
        assertThat(statusOf(trace, "span-3")).isEqualTo("ERROR");
    }

    @Test
    void should_use_root_span_name_for_root_operation() {
        TempoTraceResponse response = singleBatch(
            "gateway",
            spanWithStatus("trace", "root", null, "GET /proxy", "0"),
            spanWithStatus("trace", "child", "root", "callout", "0")
        );
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(response));

        Trace trace = underTest.getTrace(TracingQueryContext.EMPTY, "trace").blockingGet();

        assertThat(trace).isNotNull();
        assertThat(trace.rootService()).isEqualTo("gateway");
        assertThat(trace.rootOperation()).isEqualTo("GET /proxy");
    }

    @Test
    void should_forward_context_tenant_to_http_client_on_search_and_get() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(new TempoTraceResponse(List.of())));

        underTest.searchTraces(TracingQueryContext.forTenant("acme"), new TraceSearchCriteria(Map.of(), 20, null, null)).blockingGet();
        underTest.getTrace(TracingQueryContext.forTenant("acme"), "trace").blockingGet();

        ArgumentCaptor<String> searchTenantCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(any(), any(), any(), any(), searchTenantCaptor.capture());
        assertThat(searchTenantCaptor.getAllValues()).containsExactly("acme", "acme");

        ArgumentCaptor<String> getTenantCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient).getTrace(eq("trace"), getTenantCaptor.capture());
        assertThat(getTenantCaptor.getValue()).isEqualTo("acme");
    }

    @Test
    void should_pass_null_tenant_when_context_is_empty() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        underTest.searchTraces(TracingQueryContext.EMPTY, new TraceSearchCriteria(Map.of(), 20, null, null)).blockingGet();

        ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(any(), any(), any(), any(), tenantCaptor.capture());
        assertThat(tenantCaptor.getAllValues()).containsOnlyNulls();
    }

    @Test
    void should_inject_resource_attribute_filters_into_traceql_when_set() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        underTest
            .searchTraces(
                TracingQueryContext.of("acme", Map.of("gravitee.environment.id", "production")),
                new TraceSearchCriteria(Map.of("http.method", "GET"), 20, null, null)
            )
            .blockingGet();

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(queryCaptor.capture(), any(), any(), any(), any());
        String primary = queryCaptor.getAllValues().get(0);
        // Resource-attribute clauses come first so the caller-driven scope is always present, even when no
        // attribute filters are passed.
        assertThat(primary).contains("resource.gravitee.environment.id = \"production\"");
        assertThat(primary).contains("span.http.method = \"GET\"");
    }

    @Test
    void should_combine_multiple_resource_attribute_filters_with_and() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        // Use a LinkedHashMap so the clause order in the assertion is stable.
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("gravitee.environment.id", "production");
        filters.put("cloud.region", "eu-west-1");
        underTest.searchTraces(TracingQueryContext.of("acme", filters), new TraceSearchCriteria(Map.of(), 20, null, null)).blockingGet();

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(queryCaptor.capture(), any(), any(), any(), any());
        String primary = queryCaptor.getAllValues().get(0);
        assertThat(primary).contains("resource.gravitee.environment.id = \"production\"");
        assertThat(primary).contains("resource.cloud.region = \"eu-west-1\"");
        assertThat(primary).contains(" && ");
    }

    @Test
    void should_omit_resource_attribute_clauses_when_map_is_empty() {
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        underTest.searchTraces(TracingQueryContext.forTenant("acme"), new TraceSearchCriteria(Map.of(), 20, null, null)).blockingGet();

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(queryCaptor.capture(), any(), any(), any(), any());
        assertThat(queryCaptor.getAllValues().get(0)).doesNotContain("resource.");
    }

    @Test
    void should_treat_get_trace_as_not_found_when_resource_attribute_does_not_match() {
        // Trace exists in Tempo but its resource attribute says env = staging — caller asking for production must
        // see Maybe.empty(), not the staging trace.
        TempoTraceResponse response = singleBatchWithEnv("gateway", "staging", spanWithStatus("trace", "root", null, "GET /", "0"));
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(response));

        Trace trace = underTest
            .getTrace(TracingQueryContext.of("acme", Map.of("gravitee.environment.id", "production")), "trace")
            .blockingGet();

        assertThat(trace).isNull();
    }

    @Test
    void should_return_get_trace_when_all_resource_attributes_match_some_batch() {
        TempoTraceResponse response = singleBatchWithEnv("gateway", "production", spanWithStatus("trace", "root", null, "GET /", "0"));
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(response));

        Trace trace = underTest
            .getTrace(TracingQueryContext.of("acme", Map.of("gravitee.environment.id", "production")), "trace")
            .blockingGet();

        assertThat(trace).isNotNull();
        assertThat(trace.traceId()).isEqualTo("trace");
    }

    @Test
    void should_return_get_trace_when_at_least_one_batch_satisfies_all_filters() {
        // Two batches: one Gravitee-emitted with env=production, one external service with no env attribute. The
        // "any batch satisfies all filters" semantic must accept this trace — external service spans don't carry
        // Gravitee resource attributes and shouldn't block the match.
        TempoTraceResponse.ResourceSpans gateway = new TempoTraceResponse.ResourceSpans(
            new TempoTraceResponse.Resource(
                List.of(
                    new TempoTraceResponse.KeyValue("service.name", strVal("gateway")),
                    new TempoTraceResponse.KeyValue("gravitee.environment.id", strVal("production"))
                )
            ),
            List.of(
                new TempoTraceResponse.ScopeSpans(
                    new TempoTraceResponse.Scope("scope"),
                    List.of(spanWithStatus("trace", "root", null, "GET /", "0"))
                )
            )
        );
        TempoTraceResponse.ResourceSpans external = new TempoTraceResponse.ResourceSpans(
            new TempoTraceResponse.Resource(List.of(new TempoTraceResponse.KeyValue("service.name", strVal("upstream")))),
            List.of(
                new TempoTraceResponse.ScopeSpans(
                    new TempoTraceResponse.Scope("scope"),
                    List.of(spanWithStatus("trace", "child", "root", "POST /", "0"))
                )
            )
        );
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(new TempoTraceResponse(List.of(gateway, external))));

        Trace trace = underTest
            .getTrace(TracingQueryContext.of("acme", Map.of("gravitee.environment.id", "production")), "trace")
            .blockingGet();

        assertThat(trace).isNotNull();
    }

    @Test
    void should_hex_encode_base64_ids_returned_by_tempo() {
        // Tempo's /api/traces/{id} returns OTLP/JSON with base64-encoded binary IDs. Callers (and the matching ES
        // tracing repository) expect hex. Decode the known 16-byte trace id and 8-byte span id and re-encode.
        // base64 "hLz5Lr5Nzm0HBLlwpyH7ig==" decodes to the 16 bytes for trace id "84bcf92ebe4dce6d0704b970a721fb8a".
        // base64 "dBR0f2vpGxg=" decodes to the 8 bytes for span id "7414747f6be91b18".
        // base64 "cudkhg0tvEo=" decodes to the 8 bytes for parent span id "72e764860d2dbc4a".
        TempoTraceResponse.Span span = new TempoTraceResponse.Span(
            "hLz5Lr5Nzm0HBLlwpyH7ig==",
            "dBR0f2vpGxg=",
            "cudkhg0tvEo=",
            "Request phase",
            "1700000000000000000",
            "1700000001000000000",
            List.of(),
            new TempoTraceResponse.Status("0", null),
            List.of()
        );
        TempoTraceResponse response = singleBatch("gateway", span);
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(response));

        Trace trace = underTest.getTrace(TracingQueryContext.EMPTY, "trace").blockingGet();

        assertThat(trace).isNotNull();
        assertThat(trace.spans())
            .singleElement()
            .satisfies(s -> {
                assertThat(s.traceId()).isEqualTo("84bcf92ebe4dce6d0704b970a721fb8a");
                assertThat(s.spanId()).isEqualTo("7414747f6be91b18");
                assertThat(s.parentSpanId()).isEqualTo("72e764860d2dbc4a");
            });
    }

    @Test
    void should_propagate_close_to_http_client() {
        underTest.close();
        verify(tempoClient).close();
    }

    @Test
    void should_return_mutable_collections_so_callers_can_post_process() {
        TempoTraceResponse response = singleBatch("gateway", spanWithStatus("trace", "root", null, "GET /proxy", "0"));
        when(tempoClient.getTrace(eq("trace"), any())).thenReturn(Single.just(response));

        Trace trace = underTest.getTrace(TracingQueryContext.EMPTY, "trace").blockingGet();
        assertThat(trace).isNotNull();

        // Adding to spans / attributes / events must not throw.
        trace.spans().add(null);
        trace.spans().get(0).attributes().put("custom", "value");
        trace.spans().get(0).children().add(null);
        trace.spans().get(0).events().add(null);
    }

    @Test
    void should_escape_quotes_and_backslashes_in_attribute_filter_values() {
        // Values flowing into TraceQL clauses must escape " and \ — without escaping, a value carrying either
        // breaks the query syntactically. Pin both the span-attribute and the resource-attribute paths.
        when(tempoClient.searchTracesTraceQL(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(new TempoSearchResponse(List.of())));

        Map<String, String> attributeFilters = new LinkedHashMap<>();
        attributeFilters.put("http.url", "https://api.example.com/\"with-quote\"");
        Map<String, String> resourceFilters = new LinkedHashMap<>();
        resourceFilters.put("deployment.path", "C:\\logs\\trace");
        underTest
            .searchTraces(TracingQueryContext.of(null, resourceFilters), new TraceSearchCriteria(attributeFilters, 20, null, null))
            .blockingGet();

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(tempoClient, times(2)).searchTracesTraceQL(queryCaptor.capture(), any(), any(), any(), any());
        String primary = queryCaptor.getAllValues().get(0);
        assertThat(primary).contains("span.http.url = \"https://api.example.com/\\\"with-quote\\\"\"");
        assertThat(primary).contains("resource.deployment.path = \"C:\\\\logs\\\\trace\"");
    }

    // ----- helpers ---------------------------------------------------------

    private static TempoSearchResponse.TraceResult traceResult(String id) {
        return new TempoSearchResponse.TraceResult(id, "gateway", "GET /", 1_700_000_000_000_000_000L, 10L);
    }

    private static TempoTraceResponse.Value strVal(String value) {
        return new TempoTraceResponse.Value(value, null, null, null);
    }

    private static TempoTraceResponse.Span spanWithStatus(
        String traceId,
        String spanId,
        String parentSpanId,
        String name,
        String statusCode
    ) {
        return new TempoTraceResponse.Span(
            traceId,
            spanId,
            parentSpanId,
            name,
            "1700000000000000000",
            "1700000001000000000",
            List.of(),
            new TempoTraceResponse.Status(statusCode, null),
            List.of()
        );
    }

    private static TempoTraceResponse singleBatch(String serviceName, TempoTraceResponse.Span... spans) {
        return new TempoTraceResponse(
            List.of(
                new TempoTraceResponse.ResourceSpans(
                    new TempoTraceResponse.Resource(List.of(new TempoTraceResponse.KeyValue("service.name", strVal(serviceName)))),
                    List.of(new TempoTraceResponse.ScopeSpans(new TempoTraceResponse.Scope("scope"), List.of(spans)))
                )
            )
        );
    }

    private static TempoTraceResponse singleBatchWithEnv(String serviceName, String environment, TempoTraceResponse.Span... spans) {
        return new TempoTraceResponse(
            List.of(
                new TempoTraceResponse.ResourceSpans(
                    new TempoTraceResponse.Resource(
                        List.of(
                            new TempoTraceResponse.KeyValue("service.name", strVal(serviceName)),
                            new TempoTraceResponse.KeyValue("gravitee.environment.id", strVal(environment))
                        )
                    ),
                    List.of(new TempoTraceResponse.ScopeSpans(new TempoTraceResponse.Scope("scope"), List.of(spans)))
                )
            )
        );
    }

    private static String statusOf(Trace trace, String spanId) {
        return trace
            .spans()
            .stream()
            .filter(s -> s.spanId().equals(spanId))
            .map(TraceSpan::attributes)
            .map(a -> a.get("otel.status_code"))
            .findFirst()
            .orElse(null);
    }
}
