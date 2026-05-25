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
package io.gravitee.node.api.opentelemetry;

import io.gravitee.common.service.Service;
import io.vertx.core.Context;
import java.util.Map;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Logger extends Service<Logger> {
    /**
     * Log a body associated to this context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param body the body to be logged
     */
    void record(final Context vertxContext, String body);

    /**
     * Log a body with attributes associated to this context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param body the body to be logged
     * @param attributes the attributes to attach to the Log
     */
    void record(final Context vertxContext, String body, Map<String, Object> attributes);

    /**
     * Log a body with attributes associated to this context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span extract OpenTelemetry context from this span
     * @param body the body to be logged
     * @param attributes the attributes to attach to the Log
     */
    void record(final Context vertxContext, Span span, String body, Map<String, Object> attributes);

    /**
     * Log a body correlated with a trace by raw {@code traceId} / {@code spanId} strings. Intended for
     * callers that receive the identifiers from elsewhere (e.g. a request header or a reporter input)
     * and don't hold the originating {@link Span}. The implementation reconstructs a
     * {@code SpanContext} from the two ids so the resulting log record carries them in the OTLP
     * top-level {@code TraceId} / {@code SpanId} fields rather than as record attributes.
     *
     * <p>When the supplied identifiers are malformed (wrong hex length, non-hex characters, …) the
     * record is emitted uncorrelated — same fallback as the overloads that take no span at all — so
     * data is never silently dropped.
     *
     * @param vertxContext current vert context, used as fallback for tracing information when the
     *                     supplied identifiers are invalid
     * @param traceId 32-char lowercase-hex trace id
     * @param spanId 16-char lowercase-hex span id
     * @param body the body to be logged
     * @param attributes the attributes to attach to the Log
     */
    void record(final Context vertxContext, String traceId, String spanId, String body, Map<String, Object> attributes);
}
