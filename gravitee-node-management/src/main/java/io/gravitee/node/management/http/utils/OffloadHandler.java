package io.gravitee.node.management.http.utils;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;

public class OffloadHandler {

    /**
     * Wraps a blocking operation inside Vert.x's executeBlocking.
     *
     * @param blockingHandler the handler that performs blocking code and resolves the promise
     * @return a routing handler that offloads blocking logic to a worker thread
     */
    public static Handler<RoutingContext> of(Handler<Promise<Void>> blockingHandler) {
        return ctx -> {
            ctx
                .vertx()
                .executeBlocking(blockingHandler)
                .onSuccess(v -> {
                    // Do nothing — assume the handler already finished response writing
                })
                .onFailure(t -> {
                    ctx.response().setStatusCode(500).end("Internal server error");
                });
        };
    }

    /**
     * Wraps a RoutingContext-aware blocking handler.
     *
     * @param blockingHandler the blocking logic that accepts RoutingContext and resolves the promise
     * @return a routing handler that offloads the logic
     */
    public static Handler<RoutingContext> ofCtx(BiHandler<RoutingContext, Promise<Void>> blockingHandler) {
        return ctx -> {
            if (ctx.response().ended()) {
                return;
            }
            ctx
                .vertx()
                .<Void>executeBlocking(promise -> {
                    try {
                        blockingHandler.handle(ctx, promise);
                    } catch (Throwable t) {
                        promise.tryFail(t);
                    }
                })
                .onSuccess(v -> {
                    // Endpoint is responsible for ending the response.
                })
                .onFailure(t -> {
                    if (!ctx.response().ended()) {
                        ctx.response().setStatusCode(500).end("Internal server error");
                    }
                });
        };
    }

    @FunctionalInterface
    public interface BiHandler<T, U> {
        void handle(T t, U u);
    }
}
