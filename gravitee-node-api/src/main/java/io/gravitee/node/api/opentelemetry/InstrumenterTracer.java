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

import io.vertx.core.Context;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface InstrumenterTracer {
    /**
     * @return any instrumentation name
     */
    String instrumentationName();

    /**
     * @param request request object to handle
     * @return <code>true</code> if the instrumenter is able to handle the given request object
     */
    <R> boolean canHandle(final R request);

    /**
     * Start a span based on the given parameters
     *
     * @param vertxContext current vert context used to store tracing information
     * @param request request instrumented to create the span
     * @param root indicate if the span is expected to be the root one
     * @param parentSpan any set, will be used as parent context; could be <code>null</code>
     *
     * @return started {@link Span}
     */
    <R> Span startSpan(final Context vertxContext, final R request, final boolean root, final Span parentSpan);

    /**
     * End span based on the given parameters
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span the span to end
     * @param response any object used as ending response, could be <code>null</code>
     * @param throwable throwable to attach as error details; could be <code>null</code>
     */
    <R> void endSpan(final Context vertxContext, final Span span, final R response, final Throwable throwable);
}
