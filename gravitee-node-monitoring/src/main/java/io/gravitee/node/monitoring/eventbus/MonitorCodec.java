package io.gravitee.node.monitoring.eventbus;

import io.gravitee.node.api.monitor.Monitor;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MonitorCodec implements MessageCodec<Monitor, Monitor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorCodec.class);
    public static final String CODEC_NAME = "gio:bus:codec:monitor";

    @Override
    public void encodeToWire(Buffer buffer, Monitor monitor) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(monitor);
            oos.flush();

            byte[] data = bos.toByteArray();
            int length = data.length;

            // Write data into given buffer
            buffer.appendInt(length);
            buffer.appendBytes(data);
        } catch (final Exception ex) {
            LOGGER.error("Error while trying to encode a Monitor object", ex);
        }
    }

    @Override
    public Monitor decodeFromWire(int position, Buffer buffer) {
        try {
            // My custom message starting from this *position* of buffer
            int pos = position;

            // Length of data
            int length = buffer.getInt(pos);

            pos += 4;
            final int start = pos;
            final int end = pos + length;
            byte[] data = buffer.getBytes(start, end);

            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return (Monitor) is.readObject();
        } catch (Exception ex) {
            LOGGER.error("Error while trying to decode a Monitor object", ex);
        }

        return null;
    }

    @Override
    public Monitor transform(Monitor monitor) {
        // If a message is sent *locally* across the event bus.
        // This example sends message just as is
        return monitor;
    }

    @Override
    public String name() {
        return CODEC_NAME;
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}