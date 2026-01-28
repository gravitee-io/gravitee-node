/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.management.http.metrics.prometheus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PrometheusEndpointTest {

    private PrometheusEndpoint cut;

    @Mock
    private RoutingContext routingContext;

    @Mock
    private HttpServerResponse httpServerResponse;

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private HttpConnection httpConnection;

    @Mock
    private Vertx vertx;

    @BeforeEach
    void init() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        // Add a sample metric for testing
        prometheusRegistry.counter("test_counter", "env", "test").increment();

        cut = new PrometheusEndpoint(prometheusRegistry);
    }

    private void setupHandleMocks() {
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(routingContext.vertx()).thenReturn(vertx);
        when(httpServerResponse.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(httpServerResponse);
        when(httpServerResponse.setChunked(anyBoolean())).thenReturn(httpServerResponse);
    }

    @Test
    void should_return_correct_path() {
        assertThat(cut.path()).isEqualTo("/metrics/prometheus");
    }

    @Test
    void should_return_get_method() {
        assertThat(cut.method()).isEqualTo(io.gravitee.common.http.HttpMethod.GET);
    }

    @Test
    void should_set_correct_content_type_and_chunked_mode() {
        setupHandleMocks();
        when(vertx.executeBlocking(any(Callable.class))).thenReturn(Future.succeededFuture());

        cut.handle(routingContext);

        verify(httpServerResponse).putHeader(CONTENT_TYPE, PrometheusEndpoint.CONTENT_TYPE_004);
        verify(httpServerResponse).setChunked(true);
    }

    @Test
    void should_scrape_metrics_and_end_response_on_success() {
        setupHandleMocks();
        // Capture the callable passed to executeBlocking
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callable<Void>> callableCaptor = ArgumentCaptor.forClass(Callable.class);

        when(vertx.executeBlocking(callableCaptor.capture()))
            .thenAnswer(invocation -> {
                // Execute the callable synchronously for testing
                try {
                    callableCaptor.getValue().call();
                    return Future.succeededFuture();
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            });
        when(httpServerResponse.ended()).thenReturn(false);
        when(httpServerResponse.write(any(io.vertx.core.buffer.Buffer.class))).thenReturn(Future.succeededFuture());

        cut.handle(routingContext);

        // Verify that data was written to the response (metrics were scraped)
        verify(httpServerResponse, atLeastOnce()).write(any(io.vertx.core.buffer.Buffer.class));
        // Verify that end was called
        verify(httpServerResponse).end();
    }

    @Test
    void should_not_call_end_if_response_already_ended() {
        setupHandleMocks();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callable<Void>> callableCaptor = ArgumentCaptor.forClass(Callable.class);

        when(vertx.executeBlocking(callableCaptor.capture()))
            .thenAnswer(invocation -> {
                try {
                    callableCaptor.getValue().call();
                    return Future.succeededFuture();
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            });
        when(httpServerResponse.ended()).thenReturn(true);
        when(httpServerResponse.write(any(io.vertx.core.buffer.Buffer.class))).thenReturn(Future.succeededFuture());

        cut.handle(routingContext);

        verify(httpServerResponse, never()).end();
    }

    @Test
    void should_close_connection_on_scrape_failure() {
        setupHandleMocks();
        when(vertx.executeBlocking(any(Callable.class))).thenReturn(Future.failedFuture(new IOException("Scrape failed")));
        when(httpServerResponse.ended()).thenReturn(false);
        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.connection()).thenReturn(httpConnection);

        cut.handle(routingContext);

        verify(httpConnection).close();
        verify(httpServerResponse, never()).end();
    }

    @Test
    void should_not_close_connection_if_response_already_ended_on_failure() {
        setupHandleMocks();
        when(vertx.executeBlocking(any(Callable.class))).thenReturn(Future.failedFuture(new IOException("Scrape failed")));
        when(httpServerResponse.ended()).thenReturn(true);

        cut.handle(routingContext);

        verify(routingContext, never()).request();
        verify(httpServerResponse, never()).end();
    }

    @Test
    void should_stream_metrics_content() {
        setupHandleMocks();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callable<Void>> callableCaptor = ArgumentCaptor.forClass(Callable.class);
        ArgumentCaptor<io.vertx.core.buffer.Buffer> bufferCaptor = ArgumentCaptor.forClass(io.vertx.core.buffer.Buffer.class);

        when(vertx.executeBlocking(callableCaptor.capture()))
            .thenAnswer(invocation -> {
                try {
                    callableCaptor.getValue().call();
                    return Future.succeededFuture();
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            });
        when(httpServerResponse.ended()).thenReturn(false);
        when(httpServerResponse.write(bufferCaptor.capture())).thenReturn(Future.succeededFuture());

        cut.handle(routingContext);

        // Verify that the scraped content contains our test metric
        String writtenContent = bufferCaptor.getAllValues().stream().map(buffer -> buffer.toString()).reduce("", String::concat);

        assertThat(writtenContent).contains("test_counter");
    }
}
