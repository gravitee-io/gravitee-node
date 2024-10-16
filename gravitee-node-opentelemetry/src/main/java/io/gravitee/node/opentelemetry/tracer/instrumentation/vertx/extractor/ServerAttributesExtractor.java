package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.VertxUtil;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class ServerAttributesExtractor implements HttpServerAttributesGetter<HttpRequest, HttpResponse> {

    @Override
    public String getNetworkProtocolName(HttpRequest request, HttpResponse response) {
        return "http";
    }

    @Override
    public String getNetworkPeerAddress(HttpRequest httpRequest, HttpResponse httpResponse) {
        if (httpRequest instanceof HttpServerRequest) {
            return VertxUtil.extractRemoteHostname((HttpServerRequest) httpRequest);
        }
        return null;
    }

    @Override
    public Integer getNetworkPeerPort(HttpRequest httpRequest, HttpResponse httpResponse) {
        if (httpRequest instanceof HttpServerRequest) {
            Long remoteHostPort = VertxUtil.extractRemoteHostPort((HttpServerRequest) httpRequest);
            if (remoteHostPort == null) {
                return null;
            }
            return remoteHostPort.intValue();
        }
        return null;
    }

    @Override
    public String getUrlPath(final HttpRequest request) {
        try {
            URI uri = new URI(request.uri());
            return uri.getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String getUrlQuery(HttpRequest request) {
        try {
            URI uri = new URI(request.uri());
            return uri.getQuery();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String getHttpRoute(final HttpRequest request) {
        return null;
    }

    @Override
    public String getUrlScheme(final HttpRequest request) {
        if (request instanceof HttpServerRequest) {
            return ((HttpServerRequest) request).scheme();
        }
        return null;
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
        return httpResponse != null ? httpResponse.statusCode() : null;
    }

    @Override
    public List<String> getHttpResponseHeader(final HttpRequest request, final HttpResponse response, final String name) {
        return response != null ? response.headers().getAll(name) : Collections.emptyList();
    }
}
