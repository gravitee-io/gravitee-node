package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.gravitee.node.opentelemetry.tracer.span.OpenTelemetrySpan;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRouteBiGetter;
import io.vertx.core.spi.observability.HttpResponse;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class RouteGetter implements HttpServerRouteBiGetter<OpenTelemetrySpan<?>, HttpResponse> {

    public static final RouteGetter INSTANCE = new RouteGetter();

    @Override
    public String get(
        final io.opentelemetry.context.Context otelContext,
        final OpenTelemetrySpan<?> requestSpan,
        final HttpResponse response
    ) {
        // RESTEasy
        String route = requestSpan.vertxContext().getLocal("UrlPathTemplate");
        if (route == null) {
            // Vert.x
            route = requestSpan.vertxContext().getLocal("VertxRoute");
        }

        if (route != null && route.length() >= 1) {
            return route;
        }

        if (response != null && HttpResponseStatus.NOT_FOUND.code() == response.statusCode()) {
            return "/*";
        }

        return null;
    }
}
