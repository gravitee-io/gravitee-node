/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.vertx.client.http;

import static io.vertx.core.http.HttpClientOptions.DEFAULT_MAX_WEBSOCKET_FRAME_SIZE;

import io.vertx.core.http.HttpClientOptions;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VertxHttpClientOptions implements Serializable {

    @Serial
    private static final long serialVersionUID = -7061411805967594667L;

    public static final int DEFAULT_HTTP2_MULTIPLEXING_LIMIT = -1;
    public static final int DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE = -1;
    public static final int DEFAULT_HTTP2_STREAM_WINDOW_SIZE = -1;
    public static final int DEFAULT_MAX_FRAME_SIZE = 16384;
    public static final int MAX_FRAME_SIZE_LOWER_BOUND = 16384;
    public static final int MAX_FRAME_SIZE_UPPER_BOUND = 16777215;
    public static final long DEFAULT_IDLE_TIMEOUT = 60000;
    public static final long DEFAULT_KEEP_ALIVE_TIMEOUT = 30000;
    public static final long DEFAULT_CONNECT_TIMEOUT = 5000;
    public static final long DEFAULT_READ_TIMEOUT = 10000;
    public static final int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 100;
    public static final boolean DEFAULT_KEEP_ALIVE = true;
    public static final boolean DEFAULT_PIPELINING = false;
    public static final boolean DEFAULT_USE_COMPRESSION = true;
    public static final boolean DEFAULT_PROPAGATE_CLIENT_ACCEPT_ENCODING = false;
    public static final boolean DEFAULT_FOLLOW_REDIRECTS = false;
    public static final boolean DEFAULT_CLEAR_TEXT_UPGRADE = true;
    public static final VertxHttpProtocolVersion DEFAULT_PROTOCOL_VERSION = VertxHttpProtocolVersion.HTTP_1_1;
    public static final int DEFAULT_MAX_WEBSOCKET_FRAME_SIZE = HttpClientOptions.DEFAULT_MAX_WEBSOCKET_FRAME_SIZE;
    public static final int DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE = HttpClientOptions.DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE;

    @Builder.Default
    private int http2MultiplexingLimit = DEFAULT_HTTP2_MULTIPLEXING_LIMIT;

    /**
     * Connection Window Size.
     * Setting the value to -1 means {@link io.vertx.core.http.Http2Settings#DEFAULT_INITIAL_WINDOW_SIZE} -> {@value io.vertx.core.http.Http2Settings#DEFAULT_INITIAL_WINDOW_SIZE}
     */
    @Builder.Default
    private int http2ConnectionWindowSize = DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE;

    /**
     * Stream Window Size.
     * Setting the value to -1 means {@link io.vertx.core.http.Http2Settings#DEFAULT_INITIAL_WINDOW_SIZE} -> {@value io.vertx.core.http.Http2Settings#DEFAULT_INITIAL_WINDOW_SIZE}
     */
    @Builder.Default
    private int http2StreamWindowSize = DEFAULT_HTTP2_STREAM_WINDOW_SIZE;

    /**
     * Max frame size (initial settings).
     * Default is the HTTP/2 spec default value: {@value DEFAULT_MAX_FRAME_SIZE}.
     * Min value is {@value MAX_FRAME_SIZE_LOWER_BOUND}.
     * Max value is {@value MAX_FRAME_SIZE_UPPER_BOUND}.
     */
    @Builder.Default
    private int http2MaxFrameSize = DEFAULT_MAX_FRAME_SIZE;

    @Builder.Default
    private long idleTimeout = DEFAULT_IDLE_TIMEOUT;

    @Builder.Default
    private long keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

    @Builder.Default
    private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    @Builder.Default
    private boolean keepAlive = DEFAULT_KEEP_ALIVE;

    @Builder.Default
    private long readTimeout = DEFAULT_READ_TIMEOUT;

    @Builder.Default
    private boolean pipelining = DEFAULT_PIPELINING;

    @Builder.Default
    private int maxConcurrentConnections = DEFAULT_MAX_CONCURRENT_CONNECTIONS;

    @Builder.Default
    private boolean useCompression = DEFAULT_USE_COMPRESSION;

    @Builder.Default
    private boolean clearTextUpgrade = DEFAULT_CLEAR_TEXT_UPGRADE;

    @Builder.Default
    private VertxHttpProtocolVersion version = DEFAULT_PROTOCOL_VERSION;

    @Builder.Default
    private int maxWebSocketFrameSize = DEFAULT_MAX_WEBSOCKET_FRAME_SIZE;

    @Builder.Default
    private int maxWebSocketMessageSize = DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE;
}
