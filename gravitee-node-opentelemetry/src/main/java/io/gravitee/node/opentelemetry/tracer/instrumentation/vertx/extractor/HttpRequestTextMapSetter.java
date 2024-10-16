package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.opentelemetry.context.propagation.TextMapSetter;
import io.vertx.core.spi.observability.HttpRequest;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class HttpRequestTextMapSetter implements TextMapSetter<HttpRequest> {

    @Override
    public void set(final HttpRequest carrier, final String key, final String value) {
        if (carrier != null) {
            carrier.headers().set(key, value);
        }
    }
}
