package io.gravitee.node.management.http.utils;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static io.gravitee.node.management.http.metrics.prometheus.PrometheusEndpoint.CONTENT_TYPE_004;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;

@CustomLog
public class ConcurrencyLimitHandler implements Handler<RoutingContext> {

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
            log.warn(
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

        // bodyEndHandler, exceptionHandler, and closeHandler can all fire for one request.
        // Release the permit exactly once regardless of which lifecycle path finishes it.
        AtomicBoolean released = new AtomicBoolean(false);
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        };

        response.bodyEndHandler(v -> release.run());
        response.exceptionHandler(e -> {
            log.error("Error on connection", e);
            release.run();
        });
        response.closeHandler(v -> release.run());

        context.next();
    }
}
