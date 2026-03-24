package io.gravitee.node.management.http.metrics.prometheus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;
import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class PrometheusEndpointTest {

    @Test
    void should_end_response_after_successful_scrape() {
        PrometheusMeterRegistry mockRegistry = mock(PrometheusMeterRegistry.class);
        PrometheusEndpoint endpoint = createEndpointWith(mockRegistry);

        HttpServerResponse mockResponse = createMockResponse();
        RoutingContext mockContext = createMockContext(mockResponse);
        when(mockResponse.ended()).thenReturn(false);
        when(mockResponse.closed()).thenReturn(false);

        endpoint.handle(mockContext);

        verify(mockResponse).end();
        verify(mockResponse, never()).close();
    }

    @Test
    void should_close_response_on_scrape_IOException() throws Exception {
        PrometheusMeterRegistry mockRegistry = mock(PrometheusMeterRegistry.class);
        doThrow(new IOException("Timeout while waiting for write queue to drain")).when(mockRegistry).scrape(any(Writer.class));
        PrometheusEndpoint endpoint = createEndpointWith(mockRegistry);

        HttpServerResponse mockResponse = createMockResponse();
        RoutingContext mockContext = createMockContext(mockResponse);
        when(mockResponse.ended()).thenReturn(false);
        when(mockResponse.closed()).thenReturn(false);
        doAnswer(inv -> {
                when(mockResponse.closed()).thenReturn(true);
                return null;
            })
            .when(mockResponse)
            .close();

        endpoint.handle(mockContext);

        verify(mockResponse).close();
        verify(mockResponse, never()).end();
    }

    @Test
    void should_not_call_close_or_end_if_response_already_ended() throws Exception {
        PrometheusMeterRegistry mockRegistry = mock(PrometheusMeterRegistry.class);
        doThrow(new IOException("write error")).when(mockRegistry).scrape(any(Writer.class));
        PrometheusEndpoint endpoint = createEndpointWith(mockRegistry);

        HttpServerResponse mockResponse = createMockResponse();
        RoutingContext mockContext = createMockContext(mockResponse);
        when(mockResponse.ended()).thenReturn(true);

        endpoint.handle(mockContext);

        verify(mockResponse, never()).close();
        verify(mockResponse, never()).end();
    }

    @Test
    void should_set_chunked_and_content_type() {
        PrometheusMeterRegistry mockRegistry = mock(PrometheusMeterRegistry.class);
        PrometheusEndpoint endpoint = createEndpointWith(mockRegistry);

        HttpServerResponse mockResponse = createMockResponse();
        RoutingContext mockContext = createMockContext(mockResponse);
        when(mockResponse.ended()).thenReturn(false);
        when(mockResponse.closed()).thenReturn(false);

        endpoint.handle(mockContext);

        verify(mockResponse).setChunked(true);
        verify(mockResponse).putHeader(any(CharSequence.class), any(CharSequence.class));
    }

    @Test
    void should_return_501_when_registry_is_null() {
        PrometheusEndpoint endpoint = createEndpointWith(null);

        HttpServerResponse mockResponse = createMockResponse();
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        RoutingContext mockContext = createMockContext(mockResponse);

        endpoint.handle(mockContext);

        verify(mockResponse).setStatusCode(501);
        verify(mockResponse).end("Prometheus metrics are not enabled");
    }

    // --- Helpers ---

    private PrometheusEndpoint createEndpointWith(PrometheusMeterRegistry registry) {
        try (MockedStatic<BackendRegistries> mocked = Mockito.mockStatic(BackendRegistries.class)) {
            mocked.when(BackendRegistries::getDefaultNow).thenReturn(registry);
            return new PrometheusEndpoint();
        }
    }

    private HttpServerResponse createMockResponse() {
        HttpServerResponse resp = mock(HttpServerResponse.class);
        when(resp.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(resp);
        when(resp.setChunked(anyBoolean())).thenReturn(resp);
        return resp;
    }

    private RoutingContext createMockContext(HttpServerResponse response) {
        RoutingContext ctx = mock(RoutingContext.class);
        when(ctx.response()).thenReturn(response);
        return ctx;
    }
}
