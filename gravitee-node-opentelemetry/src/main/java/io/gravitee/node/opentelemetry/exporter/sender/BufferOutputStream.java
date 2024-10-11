package io.gravitee.node.opentelemetry.exporter.sender;

import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.io.OutputStream;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/vertx/runtime/src/main/java/io/quarkus/vertx/core/runtime/BufferOutputStream.java">BufferOutputStream.java</a>
 */
public class BufferOutputStream extends OutputStream {

    private final Buffer buffer;

    public BufferOutputStream(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.appendBytes(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        buffer.appendInt(b);
    }
}
