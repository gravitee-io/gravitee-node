package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.vertx.core.spi.observability.HttpRequest;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class HttpRequestTextMapGetter implements TextMapGetter<HttpRequest> {

    @Override
    public Iterable<String> keys(final HttpRequest carrier) {
        return carrier.headers().names();
    }

    @Override
    public String get(final HttpRequest carrier, final String key) {
        if (carrier == null) {
            return null;
        }

        return carrier.headers().get(key);
    }
}
