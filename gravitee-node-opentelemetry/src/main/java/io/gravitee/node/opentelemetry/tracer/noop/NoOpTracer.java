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
package io.gravitee.node.opentelemetry.tracer.noop;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.vertx.core.Context;
import java.util.function.BiConsumer;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpTracer extends AbstractService<Tracer> implements Tracer {

    @Override
    public <R> Span startRootSpanFrom(final Context vertxContext, final R request) {
        return NoOpSpan.asRoot();
    }

    @Override
    public <R> Span startSpanFrom(final Context vertxContext, final R request) {
        return NoOpSpan.asDefault();
    }

    @Override
    public <R> Span startSpanWithParentFrom(final Context vertxContext, final Span parentSpan, final R request) {
        return NoOpSpan.asDefault();
    }

    @Override
    public void end(final Context vertxContext, final Span span) {}

    @Override
    public void endOnError(final Context vertxContext, final Span span, final Throwable throwable) {}

    @Override
    public void endOnError(final Context vertxContext, final Span span, final String message) {}

    @Override
    public <R> void endWithResponse(final Context vertxContext, final Span span, final R response) {}

    @Override
    public <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final Throwable throwable) {}

    @Override
    public <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final String message) {}

    @Override
    public void injectSpanContext(final Context vertxContext, final BiConsumer<String, String> textMapSetter) {}

    @Override
    public void injectSpanContext(final Context vertxContext, final Span span, final BiConsumer<String, String> textMapSetter) {}
}
