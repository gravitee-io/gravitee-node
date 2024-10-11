package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.gravitee.node.api.opentelemetry.http.ObservableHttpClientRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpRequest;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.vertx.core.http.RequestOptions;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class ClientSpanNameExtractor implements SpanNameExtractor<ObservableHttpRequest> {

    private final SpanNameExtractor<ObservableHttpRequest> http;

    public ClientSpanNameExtractor(HttpClientAttributesExtractor clientAttributesExtractor) {
        this.http = HttpSpanNameExtractor.create(clientAttributesExtractor);
    }

    @Override
    public String extract(ObservableHttpRequest httpRequest) {
        if (httpRequest instanceof ObservableHttpClientRequest observableHttpClientRequest) {
            RequestOptions requestOptions = observableHttpClientRequest.requestOptions();
            if (requestOptions.getTraceOperation() != null) {
                return requestOptions.getTraceOperation();
            }
        }
        return http.extract(httpRequest);
    }
}
