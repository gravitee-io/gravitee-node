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
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractCodec<T> implements MessageCodec<T, T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCodec.class);

    private final String codecName;

    protected AbstractCodec(String codecName) {
        this.codecName = codecName;
    }

    @Override
    public void encodeToWire(Buffer buffer, T item) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(item);
            oos.flush();

            byte[] data = bos.toByteArray();
            int length = data.length;

            buffer.appendInt(length);
            buffer.appendBytes(data);
        } catch (final Exception ex) {
            LOGGER.error("Error while trying to encode a Monitor object", ex);
        }
    }

    @Override
    public T decodeFromWire(int position, Buffer buffer) {
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
            return (T) is.readObject();
        } catch (Exception ex) {
            LOGGER.error("Error while trying to decode object using codec {}", this.codecName, ex);
        }

        return null;
    }

    @Override
    public T transform(T item) {
        // If a message is sent *locally* across the event bus, just send message just as is.
        return item;
    }

    @Override
    public String name() {
        return this.codecName;
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}