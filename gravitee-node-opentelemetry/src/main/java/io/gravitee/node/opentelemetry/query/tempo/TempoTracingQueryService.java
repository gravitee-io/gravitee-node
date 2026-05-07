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

import io.gravitee.node.api.opentelemetry.query.TracingQueryService;
import io.gravitee.node.api.opentelemetry.query.model.Trace;
import io.gravitee.node.api.opentelemetry.query.model.TraceSearchCriteria;
import io.gravitee.node.api.opentelemetry.query.model.TraceSpan;
import io.gravitee.node.api.opentelemetry.query.model.TraceSpanEvent;
import io.gravitee.node.api.opentelemetry.query.model.TracingQueryContext;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tempo-backed {@link TracingQueryService}.
 *
 * @author GraviteeSource Team
 */
public class TempoTracingQueryService implements TracingQueryService, AutoCloseable {

    private final TempoHttpClient tempoClient;

    public TempoTracingQueryService(TempoHttpClient tempoClient) {
        this.tempoClient = tempoClient;
    }

    /**
     * Releases the underlying HTTP client. Wired through Spring's {@code destroyMethod="close"} so the Vert.x client's pool is
     * torn down cleanly on bean shutdown.
     */
    @Override
    public void close() {
        tempoClient.close();
    }

    @Override
    public Single<List<Trace>> searchTraces(TracingQueryContext context, TraceSearchCriteria criteria) {
        Map<String, String> resourceFilters = context != null ? context.resourceAttributeFilters() : Map.of();
        String traceQL = buildTraceQL(criteria.attributeFilters(), resourceFilters, false);
        String errorTraceQL = buildTraceQL(criteria.attributeFilters(), resourceFilters, true);
        Long start = criteria.start() != null ? criteria.start().getEpochSecond() : null;
        Long end = criteria.end() != null ? criteria.end().getEpochSecond() : null;
        if (start == null && end != null) {
            start = end - 7 * 24 * 3600L;
        }
        Long startParam = start;
        Long endParam = end;
        String tenant = context != null ? context.tenant() : null;

        Single<TempoSearchResponse> primary = tempoClient.searchTracesTraceQL(traceQL, criteria.limit(), startParam, endParam, tenant);
        // Older Tempo builds may not support the `status = error` intrinsic; fall back to an empty error set rather than
        // failing the whole list — the status column then renders as "unknown" instead of breaking the page.
        Single<Set<String>> errorIds = tempoClient
            .searchTracesTraceQL(errorTraceQL, criteria.limit(), startParam, endParam, tenant)
            .map(TempoTracingQueryService::extractTraceIds)
            .onErrorReturnItem(Set.of());

        return Single.zip(primary, errorIds, this::buildTraces);
    }

    @Override
    public Maybe<Trace> getTrace(TracingQueryContext context, String traceId) {
        String tenant = context != null ? context.tenant() : null;
        Map<String, String> resourceFilters = context != null ? context.resourceAttributeFilters() : Map.of();
        return tempoClient
            .getTrace(traceId, tenant)
            .flatMapMaybe(response -> {
                if (response == null || response.batches() == null || response.batches().isEmpty()) {
                    return Maybe.empty();
                }
                // Defence in depth: Tempo's /api/traces/{id} has no query DSL, so resource-attribute scoping has
                // to happen post-fetch. If no batch's resource carries all the requested attribute filters, treat
                // it as "not found" — a caller scoped to env A must not see env B's trace just because they know
                // its id. "At least one batch matches all filters" is intentional: traces touching external
                // services may include batches without the Gravitee-emitted attributes, and those shouldn't
                // block the match.
                if (!resourceFilters.isEmpty() && !traceMatchesResourceFilters(response, resourceFilters)) {
                    return Maybe.empty();
                }
                return Maybe.just(convertToTrace(traceId, response));
            });
    }

    private static boolean traceMatchesResourceFilters(TempoTraceResponse response, Map<String, String> filters) {
        for (TempoTraceResponse.ResourceSpans batch : response.batches()) {
            if (batch.resource() == null || batch.resource().attributes() == null) {
                continue;
            }
            if (batchSatisfies(batch.resource().attributes(), filters)) {
                return true;
            }
        }
        return false;
    }

    private static boolean batchSatisfies(List<TempoTraceResponse.KeyValue> attributes, Map<String, String> filters) {
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            boolean matched = false;
            for (TempoTraceResponse.KeyValue kv : attributes) {
                if (entry.getKey().equals(kv.key()) && kv.value() != null && entry.getValue().equals(kv.value().asString())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> extractTraceIds(TempoSearchResponse response) {
        if (response.traces() == null) {
            return Set.of();
        }
        Set<String> ids = new HashSet<>();
        for (TempoSearchResponse.TraceResult r : response.traces()) {
            ids.add(r.traceID());
        }
        return ids;
    }

    private List<Trace> buildTraces(TempoSearchResponse response, Set<String> errorTraceIds) {
        List<Trace> traces = new ArrayList<>();
        if (response.traces() != null) {
            for (TempoSearchResponse.TraceResult result : response.traces()) {
                traces.add(
                    new Trace(
                        result.traceID(),
                        Instant.ofEpochMilli(result.startTimeUnixNano() / 1_000_000),
                        result.durationMs() * 1_000_000,
                        result.rootServiceName(),
                        result.rootTraceName(),
                        errorTraceIds.contains(result.traceID()),
                        new ArrayList<>()
                    )
                );
            }
        }
        return traces;
    }

    private Trace convertToTrace(String traceId, TempoTraceResponse response) {
        List<TraceSpan> allSpans = new ArrayList<>();
        String rootService = null;
        String rootOperation = null;
        Instant startTime = null;
        long maxEndTime = 0;

        for (TempoTraceResponse.ResourceSpans batch : response.batches()) {
            String serviceName = extractServiceName(batch.resource());
            if (batch.scopeSpans() == null) continue;

            for (TempoTraceResponse.ScopeSpans scopeSpans : batch.scopeSpans()) {
                if (scopeSpans.spans() == null) continue;

                for (TempoTraceResponse.Span span : scopeSpans.spans()) {
                    TraceSpan traceSpan = convertSpan(span, serviceName);
                    allSpans.add(traceSpan);

                    if (span.parentSpanId() == null || span.parentSpanId().isEmpty()) {
                        rootService = serviceName;
                        rootOperation = span.name();
                        startTime = traceSpan.startTime();
                    }

                    long endNanos = Long.parseLong(span.endTimeUnixNano());
                    if (endNanos > maxEndTime) {
                        maxEndTime = endNanos;
                    }
                }
            }
        }

        long durationNanos = 0;
        if (startTime != null) {
            long startNanos = startTime.getEpochSecond() * 1_000_000_000 + startTime.getNano();
            durationNanos = maxEndTime - startNanos;
        }

        boolean hasError = allSpans.stream().anyMatch(s -> "ERROR".equals(s.attributes().get("otel.status_code")));

        return new Trace(traceId, startTime, durationNanos, rootService, rootOperation, hasError, allSpans);
    }

    private TraceSpan convertSpan(TempoTraceResponse.Span span, String serviceName) {
        Map<String, String> attrs = new HashMap<>();
        if (span.attributes() != null) {
            for (TempoTraceResponse.KeyValue kv : span.attributes()) {
                String value = kv.value() != null ? kv.value().asString() : null;
                if (value != null) {
                    attrs.put(kv.key(), value);
                }
            }
        }

        // OTLP status codes: 0=UNSET, 1=OK, 2=ERROR. Tempo serialises them either as the numeric index ("0"/"1"/"2") or as the
        // proto enum name ("STATUS_CODE_UNSET"/"STATUS_CODE_OK"/"STATUS_CODE_ERROR") depending on the version, so both forms
        // are normalised here.
        if (span.status() != null && span.status().code() != null) {
            String statusCode =
                switch (span.status().code()) {
                    case "0", "STATUS_CODE_UNSET" -> "UNSET";
                    case "1", "STATUS_CODE_OK" -> "OK";
                    case "2", "STATUS_CODE_ERROR" -> "ERROR";
                    default -> span.status().code();
                };
            attrs.put("otel.status_code", statusCode);
        }

        long startNanos = Long.parseLong(span.startTimeUnixNano());
        long endNanos = Long.parseLong(span.endTimeUnixNano());
        long durationNanos = endNanos - startNanos;

        Instant startTime = Instant.ofEpochSecond(startNanos / 1_000_000_000, startNanos % 1_000_000_000);

        List<TraceSpanEvent> events = convertEvents(span.events());

        return TraceSpan.of(
            hexEncodeIfBase64(span.traceId()),
            hexEncodeIfBase64(span.spanId()),
            hexEncodeIfBase64(span.parentSpanId()),
            span.name(),
            serviceName,
            startTime,
            durationNanos,
            attrs,
            events
        );
    }

    /**
     * Tempo's {@code /api/traces/{id}} returns OTLP/JSON where 8-byte / 16-byte span and trace IDs are encoded as
     * base64. Most consumers (Elasticsearch-backed indexes, OTel tooling, distributed-tracing UIs) expect IDs in the
     * conventional lowercase-hex form. Decode and re-encode so callers comparing IDs across backends see the same
     * representation. Returns the input unchanged when it doesn't decode cleanly to 8 or 16 bytes — covers the
     * unusual case where a Tempo build already hands back hex strings.
     */
    private static String hexEncodeIfBase64(String maybeBase64) {
        if (maybeBase64 == null || maybeBase64.isEmpty()) {
            return maybeBase64;
        }
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(maybeBase64);
            if (bytes.length != 8 && bytes.length != 16) {
                return maybeBase64;
            }
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (IllegalArgumentException notBase64) {
            return maybeBase64;
        }
    }

    private List<TraceSpanEvent> convertEvents(List<TempoTraceResponse.Event> rawEvents) {
        if (rawEvents == null || rawEvents.isEmpty()) {
            return new ArrayList<>();
        }
        List<TraceSpanEvent> events = new ArrayList<>(rawEvents.size());
        for (TempoTraceResponse.Event rawEvent : rawEvents) {
            Map<String, String> eventAttrs = new LinkedHashMap<>();
            if (rawEvent.attributes() != null) {
                for (TempoTraceResponse.KeyValue kv : rawEvent.attributes()) {
                    String value = kv.value() != null ? kv.value().asString() : null;
                    if (value != null) {
                        eventAttrs.put(kv.key(), value);
                    }
                }
            }
            Instant eventTime = null;
            if (rawEvent.timeUnixNano() != null) {
                long nanos = Long.parseLong(rawEvent.timeUnixNano());
                eventTime = Instant.ofEpochSecond(nanos / 1_000_000_000, nanos % 1_000_000_000);
            }
            events.add(new TraceSpanEvent(rawEvent.name(), eventTime, eventAttrs));
        }
        return events;
    }

    /**
     * Turn a structured attribute filter + caller-provided resource-attribute filters into TraceQL.
     * <ul>
     *   <li>{@code resourceFilters} entries are emitted as {@code resource.<key> = "<value>"} clauses; the caller
     *       has already decided the key belongs on the resource (e.g. tenant-specific scoping attributes).</li>
     *   <li>{@code attributeFilters} keys prefixed with {@code service.} or {@code telemetry.} go on the resource,
     *       everything else on the span.</li>
     *   <li>Multiple clauses are joined with {@code &&}.</li>
     *   <li>When {@code errorsOnly} is true, a {@code status = error} intrinsic is added so only traces with at
     *       least one errored span match.</li>
     * </ul>
     */
    private String buildTraceQL(Map<String, String> attributeFilters, Map<String, String> resourceFilters, boolean errorsOnly) {
        StringBuilder sb = new StringBuilder("{ ");
        boolean hasClause = false;
        if (resourceFilters != null) {
            for (Map.Entry<String, String> entry : resourceFilters.entrySet()) {
                if (hasClause) sb.append(" && ");
                appendClause(sb, "resource.", entry.getKey(), entry.getValue());
                hasClause = true;
            }
        }
        if (attributeFilters != null && !attributeFilters.isEmpty()) {
            for (Map.Entry<String, String> entry : attributeFilters.entrySet()) {
                if (hasClause) sb.append(" && ");
                String key = entry.getKey();
                String prefix = isResourceAttribute(key) ? "resource." : "span.";
                appendClause(sb, prefix, key, entry.getValue());
                hasClause = true;
            }
        }
        if (errorsOnly) {
            if (hasClause) sb.append(" && ");
            sb.append("status = error");
        }
        sb.append(" }");
        return sb.toString();
    }

    private static void appendClause(StringBuilder sb, String prefix, String key, String value) {
        sb.append(prefix).append(key).append(" = \"").append(escapeTraceQLValue(value)).append("\"");
    }

    /**
     * Escapes backslashes and double quotes in a TraceQL string literal so the resulting query parses cleanly.
     * Today's callers pass UUIDs / opaque identifiers without special characters, but a value flowing in from
     * outside this module (operator config, future user-supplied attribute filter) could carry either — without
     * escaping, the produced TraceQL would be syntactically invalid.
     */
    private static String escapeTraceQLValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isResourceAttribute(String key) {
        return key.startsWith("service.") || key.startsWith("telemetry.");
    }

    private String extractServiceName(TempoTraceResponse.Resource resource) {
        if (resource != null && resource.attributes() != null) {
            for (TempoTraceResponse.KeyValue kv : resource.attributes()) {
                if ("service.name".equals(kv.key()) && kv.value() != null) {
                    return kv.value().asString();
                }
            }
        }
        return "unknown";
    }
}
