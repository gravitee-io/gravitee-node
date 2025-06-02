package io.gravitee.node.management.http.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConcurrencyLimitHandlerTest {

    private RoutingContext mockContext;
    private HttpServerResponse mockResponse;
    private HttpServerRequest mockRequest;

    @BeforeEach
    void setup() {
        mockContext = mock(RoutingContext.class);
        mockRequest = mock(HttpServerRequest.class);
        mockResponse = mock(HttpServerResponse.class);
        when(mockContext.request()).thenReturn(mockRequest);
        when(mockRequest.path()).thenReturn("/test");
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(mockResponse);
        when(mockResponse.endHandler(any())).thenReturn(mockResponse);
    }

    @Test
    void should_reject_request_when_limit_reached() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(0);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        handler.handle(mockContext);
        verify(mockResponse).setStatusCode(429);
        verify(mockResponse).end("Too Many Requests - limit of 0 for path /test");
        verify(mockContext, never()).next();
    }

    @Test
    void should_allow_request_when_slot_available() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);
        verify(mockContext).next();
        verify(mockResponse).bodyEndHandler(any());
        verify(mockResponse, never()).setStatusCode(429);
    }
}
