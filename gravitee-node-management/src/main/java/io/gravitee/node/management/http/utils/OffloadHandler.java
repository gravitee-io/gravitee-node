package io.gravitee.node.management.http.utils;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class OffloadHandler {

    /**
     * Wraps a RoutingContext-aware blocking handler.
     *
     * @param blockingHandler the blocking logic that accepts RoutingContext and resolves the promise
     * @return a routing handler that offloads the logic
     */
    public static Handler<RoutingContext> ofCtx(Handler<RoutingContext> blockingHandler) {
        return ctx -> {
            if (ctx.response().ended()) {
                return;
            }
            ctx
                .vertx()
                .<Void>executeBlocking(() -> {
                    blockingHandler.handle(ctx);

                    return null;
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
}
