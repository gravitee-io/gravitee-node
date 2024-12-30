package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.gravitee.node.api.opentelemetry.http.ObservableHttpRequest;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class HttpRequestTextMapSetter implements TextMapSetter<ObservableHttpRequest> {

    @Override
    public void set(final ObservableHttpRequest httpRequest, final String key, final String value) {
        if (httpRequest != null && httpRequest.headers() != null) {
            httpRequest.headers().set(key, value);
        }
    }
}
