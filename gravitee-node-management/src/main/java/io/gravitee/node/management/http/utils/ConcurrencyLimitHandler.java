package io.gravitee.node.management.http.utils;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static io.gravitee.node.management.http.metrics.prometheus.PrometheusEndpoint.CONTENT_TYPE_004;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.concurrent.Semaphore;
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

        // Release semaphore when request ends or fails
        response.bodyEndHandler(v -> semaphore.release());
        response.exceptionHandler(e -> {
            log.error("Error thrown  ", e);
            semaphore.release();
        });

        context.next();
    }
}
