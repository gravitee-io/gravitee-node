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
import java.util.function.BiConsumer;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Tracer extends Service<Tracer> {
    /**
     * Start a span from root context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param request request instrumented to create the span
     * @return started {@link Span}
     */
    <R> Span startRootSpanFrom(final Context vertxContext, final R request);

    /**
     * Start a span and attach it to any existing current context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param request request instrumented to create the span
     * @return started {@link Span}
     */
    <R> Span startSpanFrom(final Context vertxContext, final R request);

    /**
     * Start a span and attach it to given parent span
     *
     * @param vertxContext current vert context used to store tracing information
     * @param parentSpan the span to use as parent for the newly started span
     * @param request request instrumented to create the span
     * @return started {@link Span}
     */
    <R> Span startSpanWithParentFrom(Context vertxContext, Span parentSpan, R request);

    /**
     * End the given span
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to end
     */
    void end(final Context vertxContext, final Span span);

    /**
     * End the given span on error with a {@link Throwable}
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to end
     * @param throwable the throwable associated to the error to track
     */
    void endOnError(final Context vertxContext, final Span span, final Throwable throwable);

    /**
     * End the given span on error with a error message
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to end
     * @param message the error message to attach
     */
    void endOnError(final Context vertxContext, final Span span, final String message);

    /**
     * End the given span with a response object
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to end
     * @param response the response object associated to the ending span
     */
    <R> void endWithResponse(final Context vertxContext, final Span span, final R response);

    /**
     * End the given span on error with a response object and a {@link Throwable}
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to end
     * @param response the response object associated to the ending span
     * @param throwable the throwable associated to the error to track
     */
    <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final Throwable throwable);

    /**
     * End the given span on error with a response object and an error message
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to end
     * @param response the response object associated to the ending span
     * @param message the error message to attach
     */
    <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final String message);

    /**
     * Returns the trace ID of the span currently attached to the given Vert.x context, or an empty
     * string if tracing is disabled or no span is currently attached.
     *
     * <p><b>Caveat — reliability depends on span nesting.</b> The returned ID is read from the single
     * OpenTelemetry context slot stored on the Vert.x context. That slot behaves like a stack and is
     * only guaranteed to reflect the logically active span when spans on the same Vert.x context are
     * strictly LIFO-nested (e.g., classic request/response HTTP flows). In reactors that multiplex
     * concurrent spans onto a single Vert.x context — for example the Kafka native reactor, where many
     * in-flight protocol requests share one duplicated context, or any flow that creates spans inside
     * {@code doOnSubscribe} / {@code Completable.defer} — the slot can hold a sibling span, be
     * restored to {@code null} by an out-of-order end, or otherwise not match the span the caller has
     * in mind. In those cases, resolve the specific {@link Span} the caller cares about (e.g. from a
     * context attribute) and use {@link Span#traceId()} instead.
     *
     * @param vertxContext current vert context used to store tracing information
     * @return W3C TraceContext trace ID (32 lowercase hex chars), or {@code ""} if none
     * @see Span#traceId()
     */
    String traceId(final Context vertxContext);

    /**
     * Returns the span ID of the span currently attached to the given Vert.x context, or an empty
     * string if tracing is disabled or no span is currently attached.
     *
     * <p><b>Caveat — reliability depends on span nesting.</b> Same restriction as
     * {@link #traceId(Context)}: the ID is read from a single per-context slot and is only meaningful
     * when spans on the Vert.x context are strictly LIFO-nested. In multiplexed or reactively-deferred
     * flows the slot may not match the span the caller has in mind; in those cases, resolve the
     * specific {@link Span} and use {@link Span#spanId()} instead. The trace ID is more forgiving than
     * the span ID here, since all spans of a single logical flow share one trace — the span ID is the
     * one you are most likely to get wrong.
     *
     * @param vertxContext current vert context used to store tracing information
     * @return W3C TraceContext span ID (16 lowercase hex chars), or {@code ""} if none
     * @see Span#spanId()
     */
    String spanId(final Context vertxContext);

    /**
     * Inject into the given carrier the current span context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param textMapSetter a text map setter used to set fields into carrier
     */
    void injectSpanContext(final Context vertxContext, final BiConsumer<String, String> textMapSetter);

    /**
     * Inject into the given carrier the given span context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to inject
     * @param textMapSetter a text map setter used to set fields into carrier
     */
    void injectSpanContext(final Context vertxContext, final Span span, final BiConsumer<String, String> textMapSetter);

    /**
     * Flush any pending spans to the configured exporter. Production callers should not need this — span batching is the
     * intended export model. Test code uses it to drain the BatchSpanProcessor queue eagerly so the export latency does not
     * dominate test runtime.
     * <p>
     * Default implementation is a no-op for tracers that are synchronous or do not buffer.
     */
    default void forceFlush() {}
}
