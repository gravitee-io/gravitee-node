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
package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx;

import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.opentelemetry.tracer.instrumentation.AbstractInstrumenterTracer;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor.AdditionalServerAttributesExtractor;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor.ClientSpanNameExtractor;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor.HttpClientAttributesExtractor;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor.HttpRequestTextMapGetter;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor.HttpRequestTextMapSetter;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor.RouteGetter;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor.ServerAttributesExtractor;
import io.gravitee.node.opentelemetry.tracer.span.OpenTelemetrySpan;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRouteSource;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 *
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
@RequiredArgsConstructor
public class VertxHttpInstrumenterTracer extends AbstractInstrumenterTracer<HttpRequest, HttpResponse> {

    private final OpenTelemetry openTelemetry;
    private Instrumenter<HttpRequest, HttpResponse> serverInstrumenter;
    private Instrumenter<HttpRequest, HttpResponse> clientInstrumenter;

    @Override
    public String instrumentationName() {
        return "io.gravitee.opentelemetry.http";
    }

    @Override
    public <R> boolean canHandle(final R request) {
        return request instanceof HttpRequest;
    }

    @Override
    public <R> Span startSpan(final Context vertxContext, final R request, final boolean root, final Span parent) {
        Span span = super.startSpan(vertxContext, request, root, parent);
        if (span instanceof OpenTelemetrySpan<?> requestSpan) {
            Context runningCtx = requestSpan.vertxContext();
            if (VertxContext.isDuplicatedContext(runningCtx)) {
                String pathTemplate = runningCtx.getLocal("ClientUrlPathTemplate");
                if (pathTemplate != null && !pathTemplate.isEmpty()) {
                    io.opentelemetry.api.trace.Span
                        .fromContext(requestSpan.otelContext())
                        .updateName(((HttpRequest) requestSpan.request()).method().name() + " " + pathTemplate);
                }
            }
        }
        return span;
    }

    @Override
    public <R> void endSpan(final Context vertxContext, final Span span, final R response, final Throwable throwable) {
        if (span instanceof OpenTelemetrySpan<?> requestSpan) {
            HttpServerRoute.update(
                requestSpan.otelContext(),
                HttpServerRouteSource.SERVER_FILTER,
                RouteGetter.INSTANCE,
                requestSpan,
                (HttpResponse) response
            );
        }

        super.endSpan(vertxContext, span, response, throwable);
    }

    @Override
    protected Instrumenter<HttpRequest, HttpResponse> getRootInstrumenter() {
        if (serverInstrumenter == null) {
            serverInstrumenter = createServerInstrumenter(openTelemetry);
        }
        return serverInstrumenter;
    }

    @Override
    protected Instrumenter<HttpRequest, HttpResponse> getDefaultInstrumenter() {
        if (clientInstrumenter == null) {
            clientInstrumenter = createClientInstrumenter(openTelemetry);
        }
        return clientInstrumenter;
    }

    private Instrumenter<HttpRequest, HttpResponse> createServerInstrumenter(final OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> serverBuilder = Instrumenter.builder(
            openTelemetry,
            instrumentationName(),
            HttpSpanNameExtractor.create(serverAttributesExtractor)
        );

        return serverBuilder
            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
            .addAttributesExtractor(HttpServerAttributesExtractor.create(serverAttributesExtractor))
            .addAttributesExtractor(new AdditionalServerAttributesExtractor())
            .addContextCustomizer(HttpServerRoute.create(serverAttributesExtractor))
            .buildServerInstrumenter(new HttpRequestTextMapGetter());
    }

    private Instrumenter<HttpRequest, HttpResponse> createClientInstrumenter(final OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();
        HttpClientAttributesExtractor httpClientAttributesExtractor = new HttpClientAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> clientBuilder = io.opentelemetry.instrumentation.api.instrumenter.Instrumenter.builder(
            openTelemetry,
            instrumentationName(),
            new ClientSpanNameExtractor(httpClientAttributesExtractor)
        );

        return clientBuilder
            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
            .addAttributesExtractor(
                io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor.create(httpClientAttributesExtractor)
            )
            .buildClientInstrumenter(new HttpRequestTextMapSetter());
    }
}
