package io.gravitee.node.management.http.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SafeBufferedWriterTest {

    private HttpServerResponse response;
    private SafeBufferedWriter writer;

    @BeforeEach
    void setup() {
        response = mock(HttpServerResponse.class);
        writer = new SafeBufferedWriter(response);
    }

    @Test
    void should_write_when_queue_not_full() throws IOException {
        when(response.writeQueueFull()).thenReturn(false);

        writer.write("test");

        verify(response).write("test");
    }

    @Test
    void should_wait_for_drain_when_queue_full_then_write() throws Exception {
        when(response.writeQueueFull()).thenReturn(true, false);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
                Handler<Void> handler = invocation.getArgument(0);
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        handler.handle(null);
                        latch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                    .start();
                return response;
            })
            .when(response)
            .drainHandler(any());

        Thread thread = new Thread(() -> {
            try {
                writer.write("test");
            } catch (IOException e) {
                fail("IOException should not have occurred: " + e.getMessage());
            }
        });
        thread.start();

        boolean completed = latch.await(1000, TimeUnit.MILLISECONDS);
        thread.join(1000);

        assertTrue(completed, "Drain handler was never called");
        verify(response).write("test");
    }

    @Test
    void should_timeout_if_drain_never_happens() {
        when(response.writeQueueFull()).thenReturn(true);
        when(response.drainHandler(any())).thenReturn(response);

        long start = System.currentTimeMillis();
        IOException ex = assertThrows(IOException.class, () -> writer.write("timeout"));
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 5000, "Should wait for the full timeout");
        assertEquals("Timeout while waiting for write queue to drain", ex.getMessage());
    }

    @Test
    void should_interrupt_if_thread_interrupted() {
        when(response.writeQueueFull()).thenReturn(true);
        when(response.drainHandler(any())).thenReturn(response);

        Thread.currentThread().interrupt();
        IOException ex = assertThrows(IOException.class, () -> writer.write("interrupted"));

        assertTrue(Thread.interrupted());
        assertTrue(ex.getMessage().contains("Interrupted while waiting for write queue"));
    }

    @Test
    void should_support_char_array_write() throws IOException {
        when(response.writeQueueFull()).thenReturn(false);

        char[] data = "abc123".toCharArray();
        writer.write(data, 0, 6);

        verify(response).write("abc123");
    }

    @Test
    void should_support_single_char_write() throws IOException {
        when(response.writeQueueFull()).thenReturn(false);

        writer.write('Z');

        verify(response).write("Z");
    }
}
