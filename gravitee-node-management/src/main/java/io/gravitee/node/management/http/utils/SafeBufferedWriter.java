package io.gravitee.node.management.http.utils;

import io.vertx.core.http.HttpServerResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SafeBufferedWriter extends Writer {

    private static final int DRAIN_TIMEOUT_MS = 5000;

    private final HttpServerResponse response;

    public SafeBufferedWriter(HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String data = new String(cbuf, off, len);
        writeWithBackpressure(data);
    }

    @Override
    public void write(int c) throws IOException {
        writeWithBackpressure(Character.toString((char) c));
    }

    @Override
    public void write(String str) throws IOException {
        writeWithBackpressure(str);
    }

    private void writeWithBackpressure(String data) throws IOException {
        if (response.writeQueueFull()) {
            CountDownLatch latch = new CountDownLatch(1);
            response.drainHandler(v -> latch.countDown());

            try {
                if (!latch.await(DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    throw new IOException("Timeout while waiting for write queue to drain");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for write queue to drain", e);
            }
        }

        response.write(data);
    }

    @Override
    public void flush() throws IOException {
        // No-op — Vert.x handles flushing internally
    }

    @Override
    public void close() throws IOException {
        // No-op — caller is responsible for closing the response
    }
}
