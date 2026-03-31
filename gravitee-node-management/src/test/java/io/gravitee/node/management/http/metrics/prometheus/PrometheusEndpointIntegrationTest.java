package io.gravitee.node.management.http.metrics.prometheus;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.node.management.http.utils.ConcurrencyLimitHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class PrometheusEndpointIntegrationTest {

    private PrometheusMeterRegistry prometheusMeterRegistry;
    private Vertx vertx;
    private HttpServer server;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() {
        prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        CompositeMeterRegistry compositeMeterRegistry = new CompositeMeterRegistry();
        compositeMeterRegistry.add(prometheusMeterRegistry);

        MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
            .setEnabled(true)
            .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
            .setMicrometerRegistry(compositeMeterRegistry);

        vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(metricsOptions));
        client = vertx.createHttpClient();

        Counter.builder("test_counter").register(prometheusMeterRegistry).increment();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            await(server.close());
        }
        if (client != null) {
            client.close();
        }
        if (vertx != null) {
            await(vertx.close());
        }
    }

    @Test
    void should_expose_prometheus_metrics_end_to_end() throws Exception {
        startServer(false);

        String body = scrapeAndGetBody();
        assertNotNull(body, "Body is null");
        assertFalse(body.isEmpty(), "No data received from prometheus endpoint");
        assertTrue(body.contains("test_counter_total"), body);
    }

    @Test
    void should_not_expose_prometheus_metrics_when_disabled() throws Exception {
        Router router = Router.router(vertx);
        server = vertx.createHttpServer().requestHandler(router);
        port = listen(server);

        CompletableFuture<Integer> statusFuture = new CompletableFuture<>();
        client
            .request(HttpMethod.GET, port, "localhost", "/metrics/prometheus")
            .compose(io.vertx.core.http.HttpClientRequest::send)
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    statusFuture.complete(ar.result().statusCode());
                } else {
                    statusFuture.completeExceptionally(ar.cause());
                }
            });

        assertEquals(404, await(Future.fromCompletionStage(statusFuture)));
    }

    @Test
    void should_handle_repeated_scrapes_without_degradation() throws Exception {
        startServer(false);

        for (int i = 0; i < 20; i++) {
            int status = scrapeAndGetStatus();
            assertEquals(200, status, "Request " + i + " failed with status " + status);
        }
    }

    @Test
    void should_handle_repeated_scrapes_with_concurrency_limit() throws Exception {
        startServer(true);

        for (int i = 0; i < 20; i++) {
            int status = scrapeAndGetStatus();
            assertEquals(200, status, "Request " + i + " was rejected — concurrency slots may be leaking");
        }
    }

    @Test
    void should_scrape_with_concurrency_limit_and_return_metrics() throws Exception {
        startServer(true);

        String body = scrapeAndGetBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
        assertTrue(body.contains("test_counter_total"), body);
    }

    // --- Helpers ---

    private void startServer(boolean withConcurrencyLimit) throws Exception {
        Router router = Router.router(vertx);
        router
            .route()
            .failureHandler(ctx -> {
                Throwable t = ctx.failure();
                if (t != null) {
                    t.printStackTrace();
                }
                if (!ctx.response().ended()) {
                    ctx.response().setStatusCode(500).end();
                }
            });

        // The mock only needs to be active during construction since the
        // constructor resolves the registry once and stores it in a field.
        PrometheusEndpoint endpoint;
        try (MockedStatic<BackendRegistries> mocked = Mockito.mockStatic(BackendRegistries.class)) {
            mocked.when(BackendRegistries::getDefaultNow).thenReturn(prometheusMeterRegistry);
            endpoint = new PrometheusEndpoint();
        }

        if (withConcurrencyLimit) {
            router.route(HttpMethod.GET, endpoint.path()).handler(new ConcurrencyLimitHandler(3)).handler(ctx -> endpoint.handle(ctx));
        } else {
            router.route(HttpMethod.GET, endpoint.path()).handler(ctx -> endpoint.handle(ctx));
        }

        server = vertx.createHttpServer().requestHandler(router);
        server.exceptionHandler(Throwable::printStackTrace);
        port = listen(server);
    }

    private int scrapeAndGetStatus() throws Exception {
        CompletableFuture<Integer> statusFuture = new CompletableFuture<>();
        client
            .request(HttpMethod.GET, port, "localhost", "/metrics/prometheus")
            .compose(req -> req.send())
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    io.vertx.core.http.HttpClientResponse response = ar.result();
                    response.handler(chunk -> {});
                    response.endHandler(v -> statusFuture.complete(response.statusCode()));
                    response.exceptionHandler(statusFuture::completeExceptionally);
                } else {
                    statusFuture.completeExceptionally(ar.cause());
                }
            });
        return statusFuture.get(10, TimeUnit.SECONDS);
    }

    private String scrapeAndGetBody() throws Exception {
        CompletableFuture<String> bodyFuture = new CompletableFuture<>();
        StringBuilder bodyBuilder = new StringBuilder();

        io.vertx.core.http.HttpClientRequest request = await(client.request(HttpMethod.GET, port, "localhost", "/metrics/prometheus"));
        request
            .send()
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    io.vertx.core.http.HttpClientResponse response = ar.result();
                    if (response.statusCode() != 200) {
                        bodyFuture.completeExceptionally(new RuntimeException("Status code " + response.statusCode()));
                        return;
                    }
                    response.handler(chunk -> bodyBuilder.append(chunk.toString()));
                    response.endHandler(v -> bodyFuture.complete(bodyBuilder.toString()));
                    response.exceptionHandler(bodyFuture::completeExceptionally);
                } else {
                    bodyFuture.completeExceptionally(ar.cause());
                }
            });

        return await(Future.fromCompletionStage(bodyFuture));
    }

    private static int listen(HttpServer server) throws Exception {
        return await(server.listen(0)).actualPort();
    }

    private static <T> T await(io.vertx.core.Future<T> future) throws Exception {
        CompletableFuture<T> cf = new CompletableFuture<>();
        future.onComplete(ar -> {
            if (ar.succeeded()) {
                cf.complete(ar.result());
            } else {
                cf.completeExceptionally(ar.cause());
            }
        });
        return cf.get(10, TimeUnit.SECONDS);
    }
}
