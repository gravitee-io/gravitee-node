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
package io.gravitee.node.plugin.cluster.standalone.messaging;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class StandaloneMessageCodec implements MessageCodec<Object, Object> {

    public static final String STANDALONE_CODEC_NAME = "standalone-codec";

    @Override
    public void encodeToWire(final Buffer buffer, final Object t) {
        throw new RuntimeException("This codec isn't meant to be used in clustered mode");
    }

    @Override
    public Object decodeFromWire(final int pos, final Buffer buffer) {
        throw new RuntimeException("This codec isn't meant to be used in clustered mode");
    }

    @Override
    public Object transform(final Object t) {
        return t;
    }

    @Override
    public String name() {
        return STANDALONE_CODEC_NAME;
    }

    @Override
    public byte systemCodecID() {
        // As mentioned by the java doc, should always return -1 for a user codec.
        return -1;
    }
}
