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
package io.gravitee.node.opentelemetry.tracer.instrumentation;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.opentelemetry.InstrumenterTracer;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpSpan;
import io.gravitee.node.opentelemetry.tracer.span.OpenTelemetrySpan;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.vertx.core.Context;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractInstrumenterTracer<REQ, RESP> extends AbstractService<Tracer> implements InstrumenterTracer {

    protected abstract Instrumenter<REQ, RESP> getRootInstrumenter();

    protected abstract Instrumenter<REQ, RESP> getDefaultInstrumenter();

    @Override
    public <R> Span startSpan(final Context vertxContext, final R request, final boolean root, final Span parentSpan) {
        io.opentelemetry.context.Context parentContext;
        if (parentSpan instanceof OpenTelemetrySpan<?> openTelemetryParentSpan) {
            parentContext = openTelemetryParentSpan.otelContext();
        } else {
            parentContext = VertxContextStorage.getContext(vertxContext);
            if (parentContext == null) {
                parentContext = io.opentelemetry.context.Context.current();
            }
        }
        Instrumenter<REQ, RESP> instrumenter;
        if (root || parentContext.equals(io.opentelemetry.context.Context.root())) {
            instrumenter = getRootInstrumenter();
        } else {
            instrumenter = getDefaultInstrumenter();
        }

        if (instrumenter.shouldStart(parentContext, (REQ) request)) {
            io.opentelemetry.context.Context otelContext = instrumenter.start(parentContext, (REQ) request);
            Scope scope = VertxContextStorage.INSTANCE.attach(vertxContext, otelContext);
            return new OpenTelemetrySpan<>(vertxContext, otelContext, scope, root, request);
        }
        if (root) {
            return NoOpSpan.asRoot();
        } else {
            return NoOpSpan.asDefault();
        }
    }

    @Override
    public <R> void endSpan(final Context vertxContext, final Span span, final R response, final Throwable throwable) {
        if (span instanceof OpenTelemetrySpan<?> openTelemetrySpan) {
            Scope scope = openTelemetrySpan.scope();
            if (scope == null) {
                return;
            }

            Object request = openTelemetrySpan.request();
            Instrumenter<REQ, RESP> instrumenter;
            if (openTelemetrySpan.isRoot()) {
                instrumenter = getRootInstrumenter();
            } else {
                instrumenter = getDefaultInstrumenter();
            }
            try (scope) {
                instrumenter.end(openTelemetrySpan.otelContext(), (REQ) request, (RESP) response, throwable);
            }
        }
    }
}
