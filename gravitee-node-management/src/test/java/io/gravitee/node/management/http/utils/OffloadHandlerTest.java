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
import java.util.concurrent.Callable;
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
    ArgumentCaptor<Callable<Void>> callableArgumentCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(routingContext.vertx()).thenReturn(vertx);
        when(routingContext.response()).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
    }

    @Test
    void should_execute_blocking_handler_successfully() {
        when(vertx.executeBlocking(callableArgumentCaptor.capture())).thenReturn(Future.succeededFuture());

        var handler = OffloadHandler.ofCtx(ctx -> assertEquals(routingContext, ctx));

        handler.handle(routingContext);

        verify(response, times(1)).ended();
        verifyNoMoreInteractions(response);
    }

    @Test
    void should_execute_blocking_handler_with_failure() {
        when(vertx.<Void>executeBlocking(any(Callable.class))).thenReturn(Future.failedFuture(new RuntimeException("fail")));

        var handler = OffloadHandler.ofCtx(ctx -> {});

        handler.handle(routingContext);

        verify(response).setStatusCode(500);
        verify(response).end("Internal server error");
    }
}
