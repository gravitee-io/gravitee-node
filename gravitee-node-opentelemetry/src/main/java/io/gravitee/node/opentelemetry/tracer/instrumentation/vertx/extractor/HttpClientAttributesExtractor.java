package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.gravitee.node.api.opentelemetry.http.ObservableHttpClientRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpResponse;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.vertx.core.http.HttpVersion;
import java.util.List;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class HttpClientAttributesExtractor implements HttpClientAttributesGetter<ObservableHttpRequest, ObservableHttpResponse> {

    @Override
    public String getUrlFull(final ObservableHttpRequest request) {
        return request.absoluteURI();
    }

    @Override
    public String getHttpRequestMethod(final ObservableHttpRequest request) {
        return request.method().name();
    }

    @Override
    public List<String> getHttpRequestHeader(final ObservableHttpRequest request, final String name) {
        return request.headers().getAll(name);
    }

    @Override
    public Integer getHttpResponseStatusCode(ObservableHttpRequest httpRequest, ObservableHttpResponse httpResponse, Throwable error) {
        return httpResponse.statusCode();
    }

    @Override
    public List<String> getHttpResponseHeader(
        final ObservableHttpRequest request,
        final ObservableHttpResponse response,
        final String name
    ) {
        return response.headers().getAll(name);
    }

    @Override
    public String getServerAddress(ObservableHttpRequest httpRequest) {
        return httpRequest.remoteAddress() != null ? httpRequest.remoteAddress().hostName() : null;
    }

    @Override
    public Integer getServerPort(ObservableHttpRequest httpRequest) {
        return httpRequest.remoteAddress() != null ? httpRequest.remoteAddress().port() : null;
    }

    @Override
    public String getNetworkProtocolName(final ObservableHttpRequest request, final ObservableHttpResponse response) {
        return "http";
    }

    @Override
    public String getNetworkProtocolVersion(final ObservableHttpRequest request, final ObservableHttpResponse response) {
        return getHttpVersion(request);
    }

    private String getHttpVersion(ObservableHttpRequest request) {
        if (request instanceof ObservableHttpClientRequest observableHttpClientRequest) {
            if (observableHttpClientRequest.httpClientRequest() != null) {
                HttpVersion version = observableHttpClientRequest.httpClientRequest().version();
                if (version != null) {
                    return switch (version) {
                        case HTTP_1_0 -> "1.0";
                        case HTTP_1_1 -> "1.1";
                        case HTTP_2 -> "2.0";
                        default -> version.alpnName(); // At that point version transformation will be needed for OTel semantics // Will be executed once Vert.x supports other versions
                    };
                }
            }
        }
        return null;
    }
}
