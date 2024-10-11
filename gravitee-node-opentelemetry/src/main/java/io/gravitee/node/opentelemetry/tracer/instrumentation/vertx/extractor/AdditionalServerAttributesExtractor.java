package io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.extractor;

import static io.opentelemetry.semconv.SemanticAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_BODY_SIZE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_BODY_SIZE;

import io.gravitee.node.api.opentelemetry.http.ObservableHttpRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpResponse;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpServerRequest;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.VertxUtil;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/tracing/intrumentation/vertx/HttpInstrumenterVertxTracer.java#L428">HttpInstrumenterVertxTracer.java</a>
 */
public class AdditionalServerAttributesExtractor implements AttributesExtractor<ObservableHttpRequest, ObservableHttpResponse> {

    @Override
    public void onStart(
        final AttributesBuilder attributes,
        final io.opentelemetry.context.Context parentContext,
        final ObservableHttpRequest httpRequest
    ) {
        if (httpRequest instanceof ObservableHttpServerRequest observableServerObservableHttpRequest) {
            String clientIp = VertxUtil.extractClientIP(observableServerObservableHttpRequest.httpServerRequest());
            if (clientIp != null) {
                attributes.put(CLIENT_ADDRESS, clientIp);
            }
        }
    }

    @Override
    public void onEnd(
        final AttributesBuilder attributes,
        final io.opentelemetry.context.Context context,
        final ObservableHttpRequest httpRequest,
        final ObservableHttpResponse httpResponse,
        final Throwable error
    ) {
        attributes.put(HTTP_REQUEST_BODY_SIZE, getContentLength(httpRequest.headers()));
        if (httpResponse != null) {
            attributes.put(HTTP_RESPONSE_BODY_SIZE, getContentLength(httpResponse.headers()));
        }
    }

    private static Long getContentLength(final MultiMap headers) {
        String contentLength = headers.get(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null && contentLength.length() > 0) {
            try {
                return Long.valueOf(contentLength);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
