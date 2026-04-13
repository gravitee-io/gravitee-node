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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConcurrencyLimitHandlerTest {

    private RoutingContext mockContext;
    private HttpServerResponse mockResponse;

    @BeforeEach
    void setup() {
        mockContext = createMockContext();
    }

    @Test
    void should_reject_request_when_limit_reached() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(0);
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

    @Test
    void should_register_all_three_handlers() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        verify(mockResponse).bodyEndHandler(any());
        verify(mockResponse).exceptionHandler(any());
        verify(mockResponse).closeHandler(any());
    }

    // --- Slot recovery: prove the semaphore is actually freed after each release path ---

    @Test
    void should_accept_new_request_after_successful_completion() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> bodyEndCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).bodyEndHandler(bodyEndCaptor.capture());

        bodyEndCaptor.getValue().handle(null);

        RoutingContext secondCtx = createMockContext();
        handler.handle(secondCtx);
        verify(secondCtx).next();
    }

    @Test
    void should_accept_new_request_after_exception() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).exceptionHandler(exceptionCaptor.capture());

        exceptionCaptor.getValue().handle(new RuntimeException("connection reset"));

        RoutingContext secondCtx = createMockContext();
        handler.handle(secondCtx);
        verify(secondCtx).next();
    }

    @Test
    void should_accept_new_request_after_connection_close() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> closeCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).closeHandler(closeCaptor.capture());

        closeCaptor.getValue().handle(null);

        RoutingContext secondCtx = createMockContext();
        handler.handle(secondCtx);
        verify(secondCtx).next();
    }

    // --- Double/triple-fire guard: AtomicBoolean prevents over-release ---

    @Test
    void should_release_only_once_when_both_exception_and_close_fire() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).exceptionHandler(exceptionCaptor.capture());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> closeCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).closeHandler(closeCaptor.capture());

        exceptionCaptor.getValue().handle(new RuntimeException("error"));
        closeCaptor.getValue().handle(null);

        RoutingContext secondCtx = createMockContext();
        handler.handle(secondCtx);
        verify(secondCtx).next();

        RoutingContext thirdCtx = createMockContext();
        handler.handle(thirdCtx);
        verify(thirdCtx.response()).setStatusCode(429);
    }

    @Test
    void should_release_only_once_when_all_three_handlers_fire() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> bodyEndCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).bodyEndHandler(bodyEndCaptor.capture());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).exceptionHandler(exceptionCaptor.capture());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> closeCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockResponse).closeHandler(closeCaptor.capture());

        bodyEndCaptor.getValue().handle(null);
        exceptionCaptor.getValue().handle(new RuntimeException("late error"));
        closeCaptor.getValue().handle(null);

        RoutingContext secondCtx = createMockContext();
        handler.handle(secondCtx);
        verify(secondCtx).next();

        RoutingContext thirdCtx = createMockContext();
        handler.handle(thirdCtx);
        verify(thirdCtx.response()).setStatusCode(429);
    }

    // --- Concurrent request limit ---

    @Test
    void should_allow_up_to_limit_concurrent_requests() {
        int limit = 3;
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(limit);

        RoutingContext[] contexts = new RoutingContext[limit];
        for (int i = 0; i < limit; i++) {
            contexts[i] = createMockContext();
            handler.handle(contexts[i]);
            verify(contexts[i]).next();
        }

        RoutingContext rejected = createMockContext();
        handler.handle(rejected);
        verify(rejected.response()).setStatusCode(429);
        verify(rejected, never()).next();
    }

    @Test
    void should_accept_new_request_after_one_slot_is_freed() {
        int limit = 3;
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(limit);

        RoutingContext[] contexts = new RoutingContext[limit];
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>>[] bodyEndCaptors = new ArgumentCaptor[limit];

        for (int i = 0; i < limit; i++) {
            contexts[i] = createMockContext();
            handler.handle(contexts[i]);
            bodyEndCaptors[i] = ArgumentCaptor.forClass(Handler.class);
            verify(contexts[i].response()).bodyEndHandler(bodyEndCaptors[i].capture());
        }

        RoutingContext rejected = createMockContext();
        handler.handle(rejected);
        verify(rejected.response()).setStatusCode(429);

        bodyEndCaptors[0].getValue().handle(null);

        RoutingContext accepted = createMockContext();
        handler.handle(accepted);
        verify(accepted).next();
    }

    // --- Bug reproduction: the exact scenario from the issue ---

    @Test
    void should_not_permanently_reject_after_repeated_close_without_body_end() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(3);

        for (int i = 0; i < 3; i++) {
            RoutingContext ctx = createMockContext();
            handler.handle(ctx);
            verify(ctx).next();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Handler<Void>> closeCaptor = ArgumentCaptor.forClass(Handler.class);
            verify(ctx.response()).closeHandler(closeCaptor.capture());

            closeCaptor.getValue().handle(null);
        }

        RoutingContext nextCtx = createMockContext();
        handler.handle(nextCtx);
        verify(nextCtx).next();
        verify(nextCtx.response(), never()).setStatusCode(429);
    }

    @Test
    void should_reject_with_correct_status_and_body() {
        ConcurrencyLimitHandler handler = new ConcurrencyLimitHandler(1);
        handler.handle(mockContext);

        RoutingContext rejected = createMockContext();
        handler.handle(rejected);

        verify(rejected.response()).setStatusCode(429);
        verify(rejected.response()).end("Too Many Requests - limit of 1 for path /test");
    }

    // --- Helper ---

    private RoutingContext createMockContext() {
        RoutingContext ctx = mock(RoutingContext.class);
        HttpServerRequest req = mock(HttpServerRequest.class);
        HttpServerResponse resp = mock(HttpServerResponse.class);
        when(ctx.request()).thenReturn(req);
        when(req.path()).thenReturn("/test");
        when(ctx.response()).thenReturn(resp);
        when(resp.setStatusCode(anyInt())).thenReturn(resp);
        when(resp.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(resp);
        when(resp.putHeader(anyString(), anyString())).thenReturn(resp);
        when(resp.endHandler(any())).thenReturn(resp);

        if (this.mockContext == null) {
            this.mockResponse = resp;
            this.mockContext = ctx;
        }
        return ctx;
    }
}
