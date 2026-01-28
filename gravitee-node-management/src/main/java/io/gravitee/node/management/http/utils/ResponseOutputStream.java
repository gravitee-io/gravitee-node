/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.management.http.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An OutputStream that writes directly to a Vert.x HttpServerResponse with backpressure handling.
 * This class is designed to be used within an executeBlocking context where blocking operations are acceptable.
 */
public class ResponseOutputStream extends OutputStream {

    private static final int DRAIN_TIMEOUT_MS = 5000;

    private final HttpServerResponse response;

    public ResponseOutputStream(HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (response.closed()) {
            throw new IOException("Response is closed");
        }

        waitForDrainIfNeeded();

        response.write(Buffer.buffer().appendBytes(b, off, len));
    }

    private void waitForDrainIfNeeded() throws IOException {
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
