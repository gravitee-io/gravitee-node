package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import java.util.List;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class HttpClientAttributesExtractor implements HttpClientAttributesGetter<HttpRequest, HttpResponse> {

    @Override
    public String getUrlFull(final HttpRequest request) {
        return request.absoluteURI();
    }

    @Override
    public String getHttpRequestMethod(final HttpRequest request) {
        return request.method().name();
    }

    @Override
    public List<String> getHttpRequestHeader(final HttpRequest request, final String name) {
        return request.headers().getAll(name);
    }

    @Override
    public Integer getHttpResponseStatusCode(HttpRequest httpRequest, HttpResponse httpResponse, Throwable error) {
        return httpResponse.statusCode();
    }

    @Override
    public List<String> getHttpResponseHeader(final HttpRequest request, final HttpResponse response, final String name) {
        return response.headers().getAll(name);
    }

    @Override
    public String getServerAddress(HttpRequest httpRequest) {
        return httpRequest.remoteAddress().hostName();
    }

    @Override
    public Integer getServerPort(HttpRequest httpRequest) {
        return httpRequest.remoteAddress().port();
    }

    @Override
    public String getNetworkProtocolName(final HttpRequest request, final HttpResponse response) {
        return "http";
    }

    @Override
    public String getNetworkProtocolVersion(final HttpRequest request, final HttpResponse response) {
        return getHttpVersion(request);
    }

    private String getHttpVersion(HttpRequest request) {
        if (request instanceof HttpServerRequest) {
            HttpVersion version = ((HttpServerRequest) request).version();
            if (version != null) {
                return switch (version) {
                    case HTTP_1_0 -> "1.0";
                    case HTTP_1_1 -> "1.1";
                    case HTTP_2 -> "2.0";
                    default -> version.alpnName(); // At that point version transformation will be needed for OTel semantics // Will be executed once Vert.x supports other versions
                };
            }
        }
        return null;
    }
}
