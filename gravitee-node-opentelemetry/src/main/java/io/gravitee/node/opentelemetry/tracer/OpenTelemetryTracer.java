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
package io.gravitee.node.opentelemetry.tracer;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.opentelemetry.InstrumenterTracer;
import io.gravitee.node.api.opentelemetry.InstrumenterTracerFactory;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpSpan;
import io.gravitee.node.opentelemetry.tracer.span.OpenTelemetrySpan;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContextStorage;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.vertx.core.Context;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class OpenTelemetryTracer extends AbstractService<Tracer> implements Tracer {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final List<InstrumenterTracerFactory> instrumenterTracerFactories;
    private List<InstrumenterTracer> instrumenterTracers;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (instrumenterTracerFactories != null) {
            instrumenterTracers =
                instrumenterTracerFactories
                    .stream()
                    .map(instrumenterTracerFactory -> {
                        try {
                            return instrumenterTracerFactory.createInstrumenterTracer(openTelemetrySdk);
                        } catch (Exception e) {
                            log.warn(
                                "Unable to register extra instrumenter factory [{}]",
                                instrumenterTracerFactory.getClass().getName(),
                                e
                            );
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        openTelemetrySdk.close();
    }

    @Override
    public <R> Span startRootSpanFrom(final Context vertxContext, final R request) {
        if (instrumenterTracers != null) {
            Optional<InstrumenterTracer> first = instrumenterTracers
                .stream()
                .filter(instrumenterTracer -> instrumenterTracer.canHandle(request))
                .findFirst();
            if (first.isPresent()) {
                InstrumenterTracer instrumenterTracer = first.get();
                return instrumenterTracer.startSpan(vertxContext, request, true, null);
            }
        }
        return NoOpSpan.asRoot();
    }

    @Override
    public <R> Span startSpanFrom(final Context vertxContext, final R request) {
        if (instrumenterTracers != null) {
            Optional<InstrumenterTracer> first = instrumenterTracers
                .stream()
                .filter(instrumenterTracer -> instrumenterTracer.canHandle(request))
                .findFirst();
            if (first.isPresent()) {
                InstrumenterTracer instrumenterTracer = first.get();
                return instrumenterTracer.startSpan(vertxContext, request, false, null);
            }
        }
        return NoOpSpan.asDefault();
    }

    @Override
    public <R> Span startSpanWithParentFrom(final Context vertxContext, final Span parentSpan, final R request) {
        if (instrumenterTracers != null) {
            Optional<InstrumenterTracer> first = instrumenterTracers
                .stream()
                .filter(instrumenterTracer -> instrumenterTracer.canHandle(request))
                .findFirst();
            if (first.isPresent()) {
                InstrumenterTracer instrumenterTracer = first.get();
                return instrumenterTracer.startSpan(vertxContext, request, false, parentSpan);
            }
        }
        return NoOpSpan.asDefault();
    }

    @Override
    public void end(final Context vertxContext, final Span span) {
        endWithResponse(vertxContext, span, null);
    }

    @Override
    public void endOnError(final Context vertxContext, final Span span, final Throwable throwable) {
        endWithResponseAndError(vertxContext, span, null, throwable);
    }

    @Override
    public void endOnError(final Context vertxContext, final Span span, final String message) {
        endWithResponseAndError(vertxContext, span, null, message);
    }

    @Override
    public <R> void endWithResponse(final Context vertxContext, final Span span, final R response) {
        endWithResponseAndError(vertxContext, span, response, (Throwable) null);
    }

    @Override
    public <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final String message) {
        if (span instanceof OpenTelemetrySpan<?> openTelemetrySpan) {
            openTelemetrySpan.span().setStatus(StatusCode.ERROR, message);
            endWithResponse(vertxContext, span, response);
        }
    }

    @Override
    public <R> void endWithResponseAndError(final Context vertxContext, final Span span, final R response, final Throwable throwable) {
        if (instrumenterTracers != null) {
            if (span instanceof OpenTelemetrySpan<?> openTelemetrySpan) {
                Optional<InstrumenterTracer> first = instrumenterTracers
                    .stream()
                    .filter(instrumenterTracer -> instrumenterTracer.canHandle(openTelemetrySpan.request()))
                    .findFirst();
                if (first.isPresent()) {
                    InstrumenterTracer instrumenterTracer = first.get();
                    instrumenterTracer.endSpan(vertxContext, openTelemetrySpan, response, throwable);
                }
            }
        }
    }

    @Override
    public void injectSpanContext(final Context vertxContext, final BiConsumer<String, String> textMapSetter) {
        io.opentelemetry.context.Context currentContext = VertxContextStorage.getContext(vertxContext);
        if (currentContext != null) {
            W3CTraceContextPropagator
                .getInstance()
                .inject(currentContext, null, (carrier1, key, value) -> textMapSetter.accept(key, value));
        }
    }

    @Override
    public void injectSpanContext(final Context vertxContext, final Span span, final BiConsumer<String, String> textMapSetter) {
        if (span instanceof OpenTelemetrySpan<?> openTelemetrySpan) {
            W3CTraceContextPropagator
                .getInstance()
                .inject(openTelemetrySpan.otelContext(), null, (carrier1, key, value) -> textMapSetter.accept(key, value));
        }
    }
}
