package io.gravitee.node.management.http.metrics.prometheus;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.node.management.http.utils.ConcurrencyLimitHandler;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrometheusEndpointIntegrationTest {

    private PrometheusMeterRegistry prometheusMeterRegistry;
    private Vertx vertx;
    private HttpServer server;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() {
        prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheusMeterRegistry.counter("test_counter", "env", "test").increment();

        vertx = Vertx.vertx();
        client = vertx.createHttpClient();
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
        assertTrue(body.contains("test_counter"), body);
    }

    @Test
    void should_return_501_when_registry_is_unavailable() throws Exception {
        startServer(new PrometheusEndpoint((PrometheusMeterRegistry) null), false);

        int status = scrapeAndGetStatus();
        assertEquals(501, status);
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
            assertEquals(200, status, "Request " + i + " was rejected - concurrency slots may be leaking");
        }
    }

    @Test
    void should_scrape_with_concurrency_limit_and_return_metrics() throws Exception {
        startServer(true);

        String body = scrapeAndGetBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
        assertTrue(body.contains("test_counter"), body);
    }

    private void startServer(boolean withConcurrencyLimit) throws Exception {
        startServer(new PrometheusEndpoint(prometheusMeterRegistry), withConcurrencyLimit);
    }

    private void startServer(PrometheusEndpoint endpoint, boolean withConcurrencyLimit) throws Exception {
        Router router = Router.router(vertx);
        router
            .route()
            .failureHandler(ctx -> {
                if (!ctx.response().ended()) {
                    ctx.response().setStatusCode(500).end();
                }
            });

        var route = router.route(HttpMethod.GET, endpoint.path());
        if (withConcurrencyLimit) {
            route.handler(new ConcurrencyLimitHandler(3));
        }
        route.handler(endpoint::handle);

        server = vertx.createHttpServer().requestHandler(router);
        port = listen(server);
    }

    private int scrapeAndGetStatus() throws Exception {
        CompletableFuture<Integer> statusFuture = new CompletableFuture<>();
        client
            .request(HttpMethod.GET, port, "localhost", "/metrics/prometheus")
            .compose(req -> req.send())
            .compose(response -> response.body().map(buffer -> response.statusCode()))
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    statusFuture.complete(ar.result());
                } else {
                    statusFuture.completeExceptionally(ar.cause());
                }
            });
        return statusFuture.get(10, TimeUnit.SECONDS);
    }

    private String scrapeAndGetBody() throws Exception {
        CompletableFuture<String> bodyFuture = new CompletableFuture<>();
        StringBuilder bodyBuilder = new StringBuilder();

        await(client.request(HttpMethod.GET, port, "localhost", "/metrics/prometheus"))
            .send()
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    var response = ar.result();
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
