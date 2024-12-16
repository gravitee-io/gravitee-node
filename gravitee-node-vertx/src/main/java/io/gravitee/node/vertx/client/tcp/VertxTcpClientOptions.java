package io.gravitee.node.vertx.client.tcp;

import static io.gravitee.node.vertx.server.VertxServerOptions.DEFAULT_TCP_KEEP_ALIVE;
import static io.vertx.core.net.TCPSSLOptions.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VertxTcpClientOptions {

    public static final int DEFAULT_IDLE_TIMEOUT = 0;
    public static final int DEFAULT_READ_IDLE_TIMEOUT = 0;
    public static final int DEFAULT_WRITE_IDLE_TIMEOUT = 0;
    public static final int DEFAULT_CONNECT_TIMEOUT = 3000;
    public static final int DEFAULT_RECONNECT_ATTEMPTS = 5;
    public static final int DEFAULT_RECONNECT_INTERVAL = 1000;

    @Builder.Default
    int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    @Builder.Default
    private int reconnectAttempts = DEFAULT_RECONNECT_ATTEMPTS;

    @Builder.Default
    private int reconnectInterval = DEFAULT_RECONNECT_INTERVAL;

    @Builder.Default
    private int idleTimeout = DEFAULT_IDLE_TIMEOUT;

    @Builder.Default
    private int readIdleTimeout = DEFAULT_READ_IDLE_TIMEOUT;

    @Builder.Default
    private int writeIdleTimeout = DEFAULT_WRITE_IDLE_TIMEOUT;

    @Builder.Default
    private boolean tcpKeepAlive = DEFAULT_TCP_KEEP_ALIVE;

    @Builder.Default
    private boolean tcpCork = DEFAULT_TCP_CORK;

    @Builder.Default
    private boolean tcpFastOpen = DEFAULT_TCP_FAST_OPEN;

    @Builder.Default
    private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

    @Builder.Default
    private boolean tcpQuickAck = DEFAULT_TCP_QUICKACK;

    @Builder.Default
    private boolean reuseAddress = DEFAULT_REUSE_ADDRESS;

    @Builder.Default
    private boolean reusePort = DEFAULT_REUSE_PORT;

    @Builder.Default
    private int soLinger = DEFAULT_SO_LINGER;

    @Builder.Default
    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;

    @Builder.Default
    private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;
}
