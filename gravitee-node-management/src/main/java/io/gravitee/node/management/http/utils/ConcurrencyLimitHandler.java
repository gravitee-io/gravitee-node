package io.gravitee.node.management.http.utils;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static io.gravitee.node.management.http.metrics.prometheus.PrometheusEndpoint.CONTENT_TYPE_004;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;

@CustomLog
public class ConcurrencyLimitHandler implements Handler<RoutingContext> {

    private static final int TOO_MANY_REQUESTS = 429;
    public static final long DEFAULT_TIMEOUT_MS = 30_000;

    private final Semaphore semaphore;
    private final int maxConcurrentRequests;
    private final long timeoutMs;

    public ConcurrencyLimitHandler(int maxConcurrentRequests) {
        this(maxConcurrentRequests, DEFAULT_TIMEOUT_MS);
    }

    public ConcurrencyLimitHandler(int maxConcurrentRequests, long timeoutMs) {
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.timeoutMs = timeoutMs;
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
        AtomicLong timerId = new AtomicLong(-1);
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                context.vertx().cancelTimer(timerId.get());
                semaphore.release();
            }
        };

        // A request whose work never completes fires none of the handlers below, so the permit
        // would be lost for good. Abort it after the timeout; the worker thread may stay stuck.
        timerId.set(
            context
                .vertx()
                .setTimer(
                    timeoutMs,
                    id -> {
                        log.warn("The endpoint {} did not respond within {} ms, aborting request", context.request().path(), timeoutMs);
                        release.run();
                        if (!response.closed()) {
                            context.request().connection().close();
                        }
                    }
                )
        );

        response.bodyEndHandler(v -> release.run());
        response.exceptionHandler(e -> {
            log.error("Error on connection", e);
            release.run();
        });
        response.closeHandler(v -> release.run());

        context.next();
    }
}
