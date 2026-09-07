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
package io.gravitee.node.vertx.server.http;

import io.gravitee.node.vertx.server.VertxServerOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.core.tracing.TracingPolicy;
import java.util.Arrays;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.core.env.Environment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Getter
@SuperBuilder
public class VertxHttpServerOptions extends VertxServerOptions {

    public static final String HTTP_PREFIX = "http";
    public static final boolean DEFAULT_WEBSOCKET_ENABLED = false;
    public static final boolean DEFAULT_ALPN = false;
    public static final boolean DEFAULT_HANDLE_100_CONTINUE = false;
    public static final String DEFAULT_TRACING_POLICY = HttpServerOptions.DEFAULT_TRACING_POLICY.name();
    public static final int DEFAULT_MAX_HEADER_SIZE = HttpServerOptions.DEFAULT_MAX_HEADER_SIZE;
    public static final int DEFAULT_MAX_CHUNK_SIZE = HttpServerOptions.DEFAULT_MAX_CHUNK_SIZE;
    public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = HttpServerOptions.DEFAULT_MAX_INITIAL_LINE_LENGTH;
    public static final int DEFAULT_MAX_FORM_ATTRIBUTE_SIZE = HttpServerOptions.DEFAULT_MAX_FORM_ATTRIBUTE_SIZE;
    public static final boolean DEFAULT_COMPRESSION_SUPPORTED = HttpServerOptions.DEFAULT_COMPRESSION_SUPPORTED;
    public static final boolean DEFAULT_PER_MESSAGE_WEBSOCKET_COMPRESSION_SUPPORTED =
        HttpServerOptions.DEFAULT_PER_MESSAGE_WEBSOCKET_COMPRESSION_SUPPORTED;
    public static final boolean DEFAULT_PER_FRAME_WEBSOCKET_COMPRESSION_SUPPORTED =
        HttpServerOptions.DEFAULT_PER_FRAME_WEBSOCKET_COMPRESSION_SUPPORTED;
    public static final int DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE = HttpServerOptions.DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE;
    public static final int DEFAULT_MAX_WEBSOCKET_FRAME_SIZE = HttpServerOptions.DEFAULT_MAX_WEBSOCKET_FRAME_SIZE;
    public static final int DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE = HttpServerOptions.DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE;
    // No Vert.x equivalent: initialSettings carries the protocol default (65535) rather than a sentinel, so -1 is ours.
    public static final int DEFAULT_HTTP2_STREAM_WINDOW_SIZE = -1;

    @Builder.Default
    protected boolean alpn = DEFAULT_ALPN;

    @Builder.Default
    private String tracingPolicy = DEFAULT_TRACING_POLICY;

    @Builder.Default
    private boolean handle100Continue = DEFAULT_HANDLE_100_CONTINUE;

    @Builder.Default
    private int maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;

    @Builder.Default
    private int maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;

    @Builder.Default
    private int maxInitialLineLength = DEFAULT_MAX_INITIAL_LINE_LENGTH;

    @Builder.Default
    private int maxFormAttributeSize = DEFAULT_MAX_FORM_ATTRIBUTE_SIZE;

    @Builder.Default
    protected boolean compressionSupported = DEFAULT_COMPRESSION_SUPPORTED;

    @Builder.Default
    private boolean websocketEnabled = DEFAULT_WEBSOCKET_ENABLED;

    private String websocketSubProtocols;

    @Builder.Default
    private boolean perMessageWebSocketCompressionSupported = DEFAULT_PER_MESSAGE_WEBSOCKET_COMPRESSION_SUPPORTED;

    @Builder.Default
    private boolean perFrameWebSocketCompressionSupported = DEFAULT_PER_FRAME_WEBSOCKET_COMPRESSION_SUPPORTED;

    @Builder.Default
    private int maxWebSocketFrameSize = DEFAULT_MAX_WEBSOCKET_FRAME_SIZE;

    @Builder.Default
    private int maxWebSocketMessageSize = DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE;

    /**
     * HTTP/2 flow-control window of the whole connection, in bytes.
     * <p>
     * {@code -1} leaves the connection window at the protocol default of
     * {@value io.vertx.core.http.Http2Settings#DEFAULT_INITIAL_WINDOW_SIZE} bytes: Vert.x only resizes the connection
     * window when the value is strictly positive.
     * <p>
     * Must be set together with {@link #http2StreamWindowSize} and greater than or equal to it: this window bounds the
     * whole connection, so it stays the bottleneck as long as it is left at the default.
     */
    @Builder.Default
    private int http2ConnectionWindowSize = DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE;

    /**
     * HTTP/2 flow-control window advertised to the client for each stream, in bytes, sent as
     * {@code SETTINGS_INITIAL_WINDOW_SIZE} in the server initial settings.
     * <p>
     * {@code -1} leaves it at the protocol default of
     * {@value io.vertx.core.http.Http2Settings#DEFAULT_INITIAL_WINDOW_SIZE} bytes.
     * <p>
     * Setting this alone has no observable effect: the initial settings are applied to streams only, never to the
     * connection, so {@link #http2ConnectionWindowSize} has to be raised as well.
     */
    @Builder.Default
    private int http2StreamWindowSize = DEFAULT_HTTP2_STREAM_WINDOW_SIZE;

    public abstract static class VertxHttpServerOptionsBuilder<
        C extends VertxHttpServerOptions, B extends VertxHttpServerOptionsBuilder<C, B>
    >
        extends VertxServerOptionsBuilder<C, B> {

        @Override
        public B environment(Environment environment) {
            super.environment(environment);

            this.alpn(environment.getProperty(prefix + ".alpn", Boolean.class, DEFAULT_ALPN));
            this.tracingPolicy(environment.getProperty(prefix + ".tracingPolicy", DEFAULT_TRACING_POLICY));
            this.handle100Continue(environment.getProperty(prefix + ".handle100Continue", Boolean.class, DEFAULT_HANDLE_100_CONTINUE));
            this.maxHeaderSize(environment.getProperty(prefix + ".maxHeaderSize", Integer.class, DEFAULT_MAX_HEADER_SIZE));
            this.maxChunkSize(environment.getProperty(prefix + ".maxChunkSize", Integer.class, DEFAULT_MAX_CHUNK_SIZE));
            this.compressionSupported(
                    environment.getProperty(prefix + ".compressionSupported", Boolean.class, DEFAULT_COMPRESSION_SUPPORTED)
                );
            this.maxInitialLineLength(
                    environment.getProperty(prefix + ".maxInitialLineLength", Integer.class, DEFAULT_MAX_INITIAL_LINE_LENGTH)
                );
            this.maxFormAttributeSize(
                    environment.getProperty(prefix + ".maxFormAttributeSize", Integer.class, DEFAULT_MAX_FORM_ATTRIBUTE_SIZE)
                );

            this.compressionSupported(
                    environment.getProperty(prefix + ".compressionSupported", Boolean.class, DEFAULT_COMPRESSION_SUPPORTED)
                );
            this.websocketEnabled(environment.getProperty(prefix + ".websocket.enabled", Boolean.class, DEFAULT_WEBSOCKET_ENABLED));
            this.websocketSubProtocols(environment.getProperty(prefix + ".websocket.subProtocols"));
            this.perMessageWebSocketCompressionSupported(
                    environment.getProperty(
                        prefix + ".websocket.perMessageWebSocketCompressionSupported",
                        Boolean.class,
                        DEFAULT_PER_MESSAGE_WEBSOCKET_COMPRESSION_SUPPORTED
                    )
                );
            this.perFrameWebSocketCompressionSupported(
                    environment.getProperty(
                        prefix + ".websocket.perFrameWebSocketCompressionSupported",
                        Boolean.class,
                        DEFAULT_PER_FRAME_WEBSOCKET_COMPRESSION_SUPPORTED
                    )
                );

            this.maxWebSocketMessageSize(
                    environment.getProperty(
                        prefix + ".websocket.maxWebSocketMessageSize",
                        Integer.class,
                        DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE
                    )
                );
            this.maxWebSocketFrameSize(
                    environment.getProperty(prefix + ".websocket.maxWebSocketFrameSize", Integer.class, DEFAULT_MAX_WEBSOCKET_FRAME_SIZE)
                );

            final int connectionWindowSize = environment.getProperty(
                prefix + ".http2.connectionWindowSize",
                Integer.class,
                DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE
            );
            final int streamWindowSize = environment.getProperty(
                prefix + ".http2.streamWindowSize",
                Integer.class,
                DEFAULT_HTTP2_STREAM_WINDOW_SIZE
            );
            checkHttp2Windows(prefix, connectionWindowSize, streamWindowSize);
            this.http2ConnectionWindowSize(connectionWindowSize);
            this.http2StreamWindowSize(streamWindowSize);
            return self();
        }

        /**
         * Reports the HTTP/2 window combinations Vert.x accepts but does not apply the way the property names
         * suggest. Lives here rather than in {@link VertxHttpServerOptions#createHttpServerOptions}, which runs once
         * per server instance while {@code instances} defaults to the number of available processors: a single
         * misconfigured server would otherwise log the same line once per core. Options assembled programmatically
         * rather than from the {@link Environment} therefore skip these checks.
         */
        private static void checkHttp2Windows(String prefix, int connectionWindowSize, int streamWindowSize) {
            if (streamWindowSize < DEFAULT_HTTP2_STREAM_WINDOW_SIZE) {
                // Http2Settings#setInitialWindowSize rejects anything below MIN_INITIAL_WINDOW_SIZE, so this would
                // otherwise fail when the server instance is created, naming neither the server nor the property.
                throw new IllegalArgumentException(
                    prefix + ".http2.streamWindowSize must be 0 or greater, or -1 to keep the protocol default, but is " + streamWindowSize
                );
            }

            // The connection window is not rejected the same way because Vert.x itself treats the two differently: a
            // non-positive connection window is dropped silently, where a negative stream window is a hard failure.
            // Reject what would otherwise break startup with an unhelpful message, warn on what is merely inert.
            if (connectionWindowSize != DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE && connectionWindowSize <= 0) {
                log.warn(
                    "Server [{}] sets http2.connectionWindowSize to {}, which Vert.x ignores. The connection window stays at the protocol default of {} bytes.",
                    prefix,
                    connectionWindowSize,
                    Http2Settings.DEFAULT_INITIAL_WINDOW_SIZE
                );
            }

            if (streamWindowSize == 0) {
                log.warn(
                    "Server [{}] sets http2.streamWindowSize to 0. The server will accept HTTP/2 requests and never read their body.",
                    prefix
                );
            }

            if (streamWindowSize > 0 && connectionWindowSize == DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE) {
                log.warn(
                    "Server [{}] raises http2.streamWindowSize to {} bytes but leaves http2.connectionWindowSize at the protocol default of {} bytes, which stays the bottleneck for the whole connection. Set http2.connectionWindowSize as well, greater than or equal to the stream window.",
                    prefix,
                    streamWindowSize,
                    Http2Settings.DEFAULT_INITIAL_WINDOW_SIZE
                );
            } else if (connectionWindowSize > 0 && streamWindowSize == DEFAULT_HTTP2_STREAM_WINDOW_SIZE) {
                log.warn(
                    "Server [{}] raises http2.connectionWindowSize to {} bytes but leaves http2.streamWindowSize at the protocol default of {} bytes, so every stream stays capped there and a single-stream transfer gains nothing. Set http2.streamWindowSize as well, up to the connection window.",
                    prefix,
                    connectionWindowSize,
                    Http2Settings.DEFAULT_INITIAL_WINDOW_SIZE
                );
            } else if (streamWindowSize > 0 && connectionWindowSize > 0 && connectionWindowSize < streamWindowSize) {
                log.warn(
                    "Server [{}] sets http2.connectionWindowSize to {} bytes, below http2.streamWindowSize of {} bytes. The connection window bounds the whole connection, so it caps every stream at the lower value.",
                    prefix,
                    connectionWindowSize,
                    streamWindowSize
                );
            }
        }
    }

    public HttpServerOptions createHttpServerOptions(KeyCertOptions vertxKeyCertOptions, TrustOptions vertxTrustOptions) {
        final HttpServerOptions options = new HttpServerOptions();

        if (this.tracingPolicy != null) {
            options.setTracingPolicy(TracingPolicy.valueOf(this.tracingPolicy.toUpperCase()));
        }

        // Binding port
        options.setPort(this.port);
        options.setHost(this.host);

        setupTcp(options, vertxKeyCertOptions, vertxTrustOptions);

        if (this.secured) {
            options.setUseAlpn(alpn);
            options.setSni(sni);

            // Specify client auth (mtls).
            options.setClientAuth(ClientAuth.valueOf(clientAuth));
        }

        if (haProxyProtocol) {
            options.setUseProxyProtocol(true).setProxyProtocolTimeout(haProxyProtocolTimeout);
        }

        // Customizable configuration
        options.setHandle100ContinueAutomatically(handle100Continue);
        options.setCompressionSupported(compressionSupported);
        options.setMaxChunkSize(maxChunkSize);
        options.setMaxHeaderSize(maxHeaderSize);
        options.getInitialSettings().setMaxHeaderListSize(maxHeaderSize);
        options.setMaxInitialLineLength(maxInitialLineLength);
        options.setMaxFormAttributeSize(maxFormAttributeSize);

        // HTTP/2 flow-control windows, bounding the request body bytes a client may send before the server acknowledges
        // them. Throughput is capped by window size / round-trip time, so the 65535 bytes protocol default throttles
        // large uploads on latent networks. Both have to be set: the initial settings reach streams only, never the
        // connection, so a stream window raised on its own leaves the connection window as the bottleneck.
        if (http2ConnectionWindowSize != DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE) {
            options.setHttp2ConnectionWindowSize(http2ConnectionWindowSize);
        }

        if (http2StreamWindowSize != DEFAULT_HTTP2_STREAM_WINDOW_SIZE) {
            options.getInitialSettings().setInitialWindowSize(http2StreamWindowSize);
        }

        // Configure websocket. Note: system property 'vertx.disableWebsockets' is no longer set as it acts globally whereas we are per http server.
        if (websocketEnabled) {
            options.setMaxWebSocketFrameSize(maxWebSocketFrameSize);
            options.setMaxWebSocketMessageSize(maxWebSocketMessageSize);
            options.setPerMessageWebSocketCompressionSupported(perMessageWebSocketCompressionSupported);
            options.setPerFrameWebSocketCompressionSupported(perFrameWebSocketCompressionSupported);

            if (websocketSubProtocols != null) {
                options.setWebSocketSubProtocols(Arrays.stream(websocketSubProtocols.split(",")).map(String::trim).toList());
            }
        } else {
            // For performance considerations, disable websocket compression if websocket is disabled.
            options.setPerMessageWebSocketCompressionSupported(false);
            options.setPerFrameWebSocketCompressionSupported(false);
        }

        return options;
    }
}
