package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import io.gravitee.node.api.opentelemetry.http.ObservableHttpRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpResponse;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpServerRequest;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.VertxUtil;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.spi.observability.HttpRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class ServerAttributesExtractor implements HttpServerAttributesGetter<ObservableHttpRequest, ObservableHttpResponse> {

    @Override
    public String getNetworkProtocolName(ObservableHttpRequest request, ObservableHttpResponse response) {
        return "http";
    }

    @Override
    public String getNetworkPeerAddress(ObservableHttpRequest httpRequest, ObservableHttpResponse httpResponse) {
        if (httpRequest instanceof ObservableHttpServerRequest observableServerHttpRequest) {
            return VertxUtil.extractRemoteHostname(observableServerHttpRequest.httpServerRequest());
        }
        return null;
    }

    @Override
    public Integer getNetworkPeerPort(ObservableHttpRequest httpRequest, ObservableHttpResponse httpResponse) {
        if (httpRequest instanceof ObservableHttpServerRequest observableServerHttpRequest) {
            Long remoteHostPort = VertxUtil.extractRemoteHostPort(observableServerHttpRequest.httpServerRequest());
            if (remoteHostPort == null) {
                return null;
            }
            return remoteHostPort.intValue();
        }
        return null;
    }

    @Override
    public String getUrlPath(final ObservableHttpRequest request) {
        try {
            URI uri = new URI(request.uri());
            return uri.getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String getUrlQuery(ObservableHttpRequest request) {
        try {
            URI uri = new URI(request.uri());
            return uri.getQuery();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String getHttpRoute(final ObservableHttpRequest request) {
        return null;
    }

    @Override
    public String getUrlScheme(final ObservableHttpRequest request) {
        if (request instanceof ObservableHttpServerRequest observableServerHttpRequest) {
            return observableServerHttpRequest.httpServerRequest().scheme();
        }
        return null;
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
        return httpResponse != null ? httpResponse.statusCode() : null;
    }

    @Override
    public List<String> getHttpResponseHeader(
        final ObservableHttpRequest request,
        final ObservableHttpResponse response,
        final String name
    ) {
        return response != null ? response.headers().getAll(name) : Collections.emptyList();
    }
}
