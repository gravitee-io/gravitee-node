package io.gravitee.node.management.http.utils;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static io.prometheus.client.exporter.common.TextFormat.CONTENT_TYPE_004;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrencyLimitHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrencyLimitHandler.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private final Semaphore semaphore;
    private final int maxConcurrentRequests;

    public ConcurrencyLimitHandler(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.semaphore = new Semaphore(maxConcurrentRequests);
    }

    @Override
    public void handle(RoutingContext context) {
        if (!semaphore.tryAcquire()) {
            LOGGER.warn(
                "The endpoint rejected request due to concurrency limit of {} for path {} ",
                maxConcurrentRequests,
                context.request().path()
            );
            context
                .response()
                .setStatusCode(TOO_MANY_REQUESTS)
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_004)
                .end("Too Many Requests - limit of " + maxConcurrentRequests + " for path " + context.request().path());
            return;
        }

        HttpServerResponse response = context.response();

        // In Vert.x, bodyEndHandler, exceptionHandler, and closeHandler can all fire for the
        // same request (e.g. IOException in SafeBufferedWriter triggers exceptionHandler, then
        // PrometheusEndpoint calls response.close() which triggers closeHandler). Without a
        // guard, each fires semaphore.release() and permits accumulate beyond the configured
        // limit — permanently breaking the concurrency gate. The AtomicBoolean ensures we
        // release exactly once regardless of how many handlers fire.
        AtomicBoolean released = new AtomicBoolean(false);
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        };

        response.bodyEndHandler(v -> release.run());
        response.exceptionHandler(e -> {
            LOGGER.error("Error on connection", e);
            release.run();
        });
        // closeHandler is critical: when SafeBufferedWriter times out, PrometheusEndpoint
        // calls response.close() which does NOT trigger bodyEndHandler or exceptionHandler.
        // Without this handler the semaphore permit is never returned.
        response.closeHandler(v -> release.run());

        context.next();
    }
}
