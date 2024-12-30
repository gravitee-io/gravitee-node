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
import io.opentelemetry.context.propagation.TextMapSetter;
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
}
