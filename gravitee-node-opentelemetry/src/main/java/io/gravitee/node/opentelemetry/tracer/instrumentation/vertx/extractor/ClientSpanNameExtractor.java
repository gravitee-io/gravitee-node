package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.vertx.core.http.impl.HttpRequestHead;
import io.vertx.core.spi.observability.HttpRequest;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class ClientSpanNameExtractor implements SpanNameExtractor<HttpRequest> {

    private final SpanNameExtractor<HttpRequest> http;

    public ClientSpanNameExtractor(HttpClientAttributesExtractor clientAttributesExtractor) {
        this.http = HttpSpanNameExtractor.create(clientAttributesExtractor);
    }

    @Override
    public String extract(HttpRequest httpRequest) {
        if (httpRequest instanceof HttpRequestHead) {
            HttpRequestHead head = (HttpRequestHead) httpRequest;
            if (head.traceOperation != null) {
                return head.traceOperation;
            }
        }
        return http.extract(httpRequest);
    }
}
