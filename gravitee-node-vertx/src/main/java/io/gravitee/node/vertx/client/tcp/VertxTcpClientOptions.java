package io.gravitee.node.vertx.client.tcp;

import lombok.Data;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class VertxTcpClientOptions {

    public static final int DEFAULT_IDLE_TIMEOUT = 0;
    public static final int DEFAULT_READ_IDLE_TIMEOUT = 0;
    public static final int DEFAULT_WRITE_IDLE_TIMEOUT = 0;
    public static final int DEFAULT_CONNECT_TIMEOUT = 3000;
    public static final int DEFAULT_RECONNECT_ATTEMPTS = 5;
    public static final int DEFAULT_RECONNECT_INTERVAL = 1000;

    int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int reconnectAttempts = DEFAULT_RECONNECT_ATTEMPTS;
    private int reconnectInterval = DEFAULT_RECONNECT_INTERVAL;
    private int idleTimeout = DEFAULT_IDLE_TIMEOUT;
    private int readIdleTimeout = DEFAULT_READ_IDLE_TIMEOUT;
    private int writeIdleTimeout = DEFAULT_WRITE_IDLE_TIMEOUT;
}
