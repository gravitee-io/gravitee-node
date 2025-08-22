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
            ctx
                .vertx()
                .executeBlocking(promise -> blockingHandler.handle(ctx, promise))
                .onSuccess(v -> {
                    // Do nothing
                })
                .onFailure(t -> {
                    ctx.response().setStatusCode(500).end("Internal server error");
                });
        };
    }

    @FunctionalInterface
    public interface BiHandler<T, U> {
        void handle(RoutingContext t, Promise<Object> u);
    }
}
