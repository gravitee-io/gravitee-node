package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.gravitee.node.api.opentelemetry.http.ObservableHttpRequest;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.vertx.core.MultiMap;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class HttpRequestTextMapGetter implements TextMapGetter<ObservableHttpRequest> {

    @Override
    public Iterable<String> keys(final ObservableHttpRequest observableHttpRequest) {
        MultiMap headers = observableHttpRequest.headers();
        if (headers != null) {
            return headers.names();
        }
        return null;
    }

    @Override
    public String get(final ObservableHttpRequest observableHttpRequest, final String key) {
        if (observableHttpRequest == null) {
            return null;
        }

        MultiMap headers = observableHttpRequest.headers();
        if (headers != null) {
            return headers.get(key);
        }
        return null;
    }
}
