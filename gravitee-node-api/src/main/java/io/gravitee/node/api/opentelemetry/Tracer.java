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

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Tracer extends Service<Tracer> {
    <R> Span startRootSpanFrom(final Context vertxContext, final R request);

    <R> Span startSpanFrom(final Context vertxContext, final R request);

    <R> Span startSpanWithParentFrom(Context vertxContext, Span parentSpan, R request);

    void end(final Context vertxContext, final Span span);

    void endOnError(final Context vertxContext, final Span span, final Throwable throwable);

    void endOnError(final Context vertxContext, final Span span, final String message);

    <R> void endWithResponse(final Context vertxContext, final Span span, final R response);

    <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final Throwable throwable);

    <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final String message);
}
