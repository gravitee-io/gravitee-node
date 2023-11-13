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
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.tracing.TracingPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
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
            return self();
        }
    }

    public HttpServerOptions createHttpServerOptions() {
        final HttpServerOptions options = new HttpServerOptions();

        if (this.tracingPolicy != null) {
            options.setTracingPolicy(TracingPolicy.valueOf(this.tracingPolicy.toUpperCase()));
        }

        // Binding port
        options.setPort(this.port);
        options.setHost(this.host);

        setupTcp(options);

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
        options.setMaxInitialLineLength(maxInitialLineLength);
        options.setMaxFormAttributeSize(maxFormAttributeSize);

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
