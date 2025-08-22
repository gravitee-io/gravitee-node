package io.gravitee.node.management.http.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class OffloadHandlerTest {

    @Mock
    Vertx vertx;

    @Mock
    RoutingContext routingContext;

    @Mock
    HttpServerResponse response;

    @Captor
    ArgumentCaptor<Handler<Promise<Void>>> handlerCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(routingContext.vertx()).thenReturn(vertx);
        when(routingContext.response()).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
    }

    @Test
    void should_execute_blocking_handler_successfully() {
        Promise<Void> promise = Promise.promise();

        when(vertx.<Void>executeBlocking(handlerCaptor.capture())).thenReturn(Future.succeededFuture());

        var handler = OffloadHandler.ofCtx((ctx, p) -> {
            assertEquals(routingContext, ctx);
            p.complete();
        });

        handler.handle(routingContext);
        Handler<Promise<Void>> captured = handlerCaptor.getValue();
        captured.handle(promise);
        assertTrue(promise.future().succeeded());
    }

    @Test
    void should_execute_blocking_handler_with_failure() {
        when(vertx.<Void>executeBlocking(any(Handler.class))).thenReturn(Future.failedFuture(new RuntimeException("fail")));

        var handler = OffloadHandler.ofCtx((ctx, p) -> {});

        handler.handle(routingContext);

        verify(response).setStatusCode(500);
        verify(response).end("Internal server error");
    }

    @Test
    void should_execute_simple_blocking_handler_successfully() {
        Handler<Promise<Void>> blockingHandler = promise -> promise.complete();

        when(vertx.<Void>executeBlocking(ArgumentMatchers.<Handler<Promise<Void>>>any()))
            .thenAnswer(invocation -> {
                Handler<Promise<Void>> handler = invocation.getArgument(0);
                Promise<Void> promise = Promise.promise();
                handler.handle(promise);
                return promise.future();
            });

        var handler = OffloadHandler.of(blockingHandler);

        handler.handle(routingContext);
        verify(response, never()).setStatusCode(anyInt());
        verify(response, never()).end(anyString());
    }

    @Test
    void should_return_500_on_simple_blocking_handler_failure() {
        when(vertx.<Void>executeBlocking(ArgumentMatchers.<Handler<Promise<Void>>>any()))
            .thenReturn(Future.failedFuture(new RuntimeException("fail")));
        var handler = OffloadHandler.of(promise -> {});

        handler.handle(routingContext);
        verify(response).setStatusCode(500);
        verify(response).end("Internal server error");
    }
}
