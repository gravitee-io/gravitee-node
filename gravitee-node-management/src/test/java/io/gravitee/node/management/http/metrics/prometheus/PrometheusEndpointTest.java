package io.gravitee.node.management.http.metrics.prometheus;

import static org.mockito.Mockito.*;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.env.Environment;

class PrometheusEndpointTest {

    private PrometheusEndpoint prometheusEndpoint;
    private RoutingContext mockContext;
    private HttpServerResponse mockResponse;
    private Semaphore semaphore;

    @BeforeEach
    void setUp() {
        Environment environment = mock(Environment.class);
        semaphore = spy(new Semaphore(1)); // Only 1 allowed at a time
        prometheusEndpoint = new PrometheusEndpoint(environment); // Inject environment if needed
        prometheusEndpoint.setSemaphore(semaphore);

        mockContext = mock(RoutingContext.class);
        mockResponse = mock(HttpServerResponse.class);

        when(environment.getProperty("services.metrics.prometheus.concurrencyLimit", Integer.class)).thenReturn(1);
        //        when(mockContext.response()).thenReturn(mockResponse);
        //        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        //        when(mockResponse.setChunked(true)).thenReturn(mockResponse);
        //        when(mockResponse.endHandler(any())).thenReturn(mockResponse);
        //        when(mockResponse.exceptionHandler(any())).thenReturn(mockResponse);
    }

    @Test
    void shouldRejectWhenSemaphoreNotAvailable() {
        try (MockedStatic<BackendRegistries> mocked = mockStatic(BackendRegistries.class)) {
            when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
            when(mockContext.response()).thenReturn(mockResponse);
            when(mockResponse.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(mockResponse);
            when(mockResponse.setChunked(true)).thenReturn(mockResponse);
            doReturn(false).when(semaphore).tryAcquire();
            //HttpServerResponse mockResponse = mock(HttpServerResponse.class);
            PrometheusMeterRegistry mockRegistry = mock(PrometheusMeterRegistry.class);
            mocked.when(BackendRegistries::getDefaultNow).thenReturn(mockRegistry);

            doAnswer(invocation -> {
                    Writer writer = invocation.getArgument(0);
                    writer.write("# HELP test_metric Test metric\n# TYPE test_metric counter\ntest_metric 1\n");
                    return null;
                })
                .when(mockRegistry)
                .scrape(any(Writer.class));

            prometheusEndpoint.handle(mockContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        verify(mockResponse).setStatusCode(429);
        verify(mockResponse).putHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        verify(mockResponse).end(contains("Too Many Requests"));
    }

    @Test
    void shouldAllowWhenSemaphoreAvailable() {
        try (MockedStatic<BackendRegistries> mocked = mockStatic(BackendRegistries.class)) {
            PrometheusMeterRegistry mockRegistry = mock(PrometheusMeterRegistry.class);
            mocked.when(BackendRegistries::getDefaultNow).thenReturn(mockRegistry);

            doAnswer(invocation -> {
                    Writer writer = invocation.getArgument(0);
                    writer.write("# HELP test_metric Test metric\n# TYPE test_metric counter\ntest_metric 1\n");
                    return null;
                })
                .when(mockRegistry)
                .scrape(any(Writer.class));

            when(mockContext.response()).thenReturn(mockResponse);
            when(mockResponse.endHandler(any())).thenReturn(mockResponse);
            when(mockResponse.exceptionHandler(any())).thenReturn(mockResponse);
            doReturn(true).when(semaphore).tryAcquire();

            prometheusEndpoint.handle(mockContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        verify(mockResponse).endHandler(any());
    }
}
