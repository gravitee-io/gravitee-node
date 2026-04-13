package io.gravitee.node.management.http.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConcurrencyLimitHandlerTest {

    private RoutingContext mockContext;

    @BeforeEach
    void setup() {
        mockContext = createMockContext();
    }

    @Test
    void should_reject_request_when_limit_reached() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(0);
        handler.handle(mockContext);

        verify(mockContext.response()).setStatusCode(429);
        verify(mockContext.response()).end("Too Many Requests - limit of 0 for path /test");
        verify(mockContext, never()).next();
    }

    @Test
    void should_allow_request_when_slot_available() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);

        handler.handle(mockContext);

        verify(mockContext).next();
        verify(mockContext.response()).bodyEndHandler(any());
        verify(mockContext.response()).closeHandler(any());
        verify(mockContext.response(), never()).setStatusCode(429);
    }

    @Test
    void should_release_only_once_when_exception_and_close_fire() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockContext.response()).exceptionHandler(exceptionCaptor.capture());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> closeCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockContext.response()).closeHandler(closeCaptor.capture());

        exceptionCaptor.getValue().handle(new RuntimeException("failure"));
        closeCaptor.getValue().handle(null);

        RoutingContext nextContext = createMockContext();
        handler.handle(nextContext);
        verify(nextContext).next();

        RoutingContext rejectedContext = createMockContext();
        handler.handle(rejectedContext);
        verify(rejectedContext.response()).setStatusCode(429);
    }

    @Test
    void should_accept_request_after_close_handler_releases_slot() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> closeCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockContext.response()).closeHandler(closeCaptor.capture());

        closeCaptor.getValue().handle(null);

        RoutingContext nextContext = createMockContext();
        handler.handle(nextContext);
        verify(nextContext).next();
    }

    private RoutingContext createMockContext() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        HttpServerResponse response = mock(HttpServerResponse.class);

        when(context.request()).thenReturn(request);
        when(request.path()).thenReturn("/test");
        when(context.response()).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.endHandler(any())).thenReturn(response);

        return context;
    }
}
