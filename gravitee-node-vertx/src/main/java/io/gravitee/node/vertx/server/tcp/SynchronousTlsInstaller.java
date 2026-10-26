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
package io.gravitee.node.vertx.server.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.vertx.core.Future;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.net.NetSocketInternal;
import io.vertx.core.internal.net.SslChannelProvider;
import io.vertx.core.internal.net.SslHandshakeCompletionHandler;
import io.vertx.core.internal.tls.SslContextManager;
import io.vertx.core.internal.tls.SslContextProvider;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.ServerSSLOptions;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Starts the TLS handshake of a plain accepted socket by installing Vert.x's own server {@code SslHandler} in the
 * connect handler, before the event loop reads anything from the socket.
 * <p>
 * The public {@code NetSocket#upgradeToSsl()} cannot be used: it resolves the SSL context asynchronously, and a
 * ClientHello read in the meantime is queued by Vert.x instead of reaching the {@code SslHandler}, so the client
 * waits forever for its ServerHello. This class relies on Vert.x internal API (checked against Vert.x 5.0.12) and
 * must be re-validated on every Vert.x upgrade.
 */
final class SynchronousTlsInstaller {

    private final VertxInternal vertx;
    private final ServerSSLOptions sslOptions;
    private final long handshakeTimeout;
    private final TimeUnit handshakeTimeoutUnit;
    private final Future<SslContextProvider> provider;

    SynchronousTlsInstaller(VertxInternal vertx, NetServerOptions options) {
        this.vertx = vertx;
        this.sslOptions = options.getSslOptions();
        this.handshakeTimeout = options.getSslHandshakeTimeout();
        this.handshakeTimeoutUnit = options.getSslHandshakeTimeoutUnit();
        final SslContextManager manager = new SslContextManager(
            SslContextManager.resolveEngineOptions(options.getSslEngineOptions(), options.isUseAlpn())
        );
        this.provider = manager.resolveSslContextProvider(
            sslOptions,
            null,
            sslOptions.getClientAuth(),
            sslOptions.getApplicationLayerProtocols(),
            vertx.getOrCreateContext()
        );
    }

    /**
     * Completes when the SSL context is ready; the server must not listen before.
     */
    Future<SslContextProvider> ready() {
        return provider;
    }

    /**
     * Adds the TLS handlers to the socket. Must be called from the connect handler, on the socket's event loop.
     *
     * @param onHandshakeSuccess called once the handshake succeeded; the socket is closed if it failed.
     */
    void install(NetSocketInternal socket, Runnable onHandshakeSuccess) {
        final ChannelHandlerContext context = socket.channelHandlerContext();
        final ChannelPipeline pipeline = context.pipeline();
        final SslChannelProvider channelProvider = new SslChannelProvider(vertx, provider.result(), sslOptions.isSni());
        final InetSocketAddress remote = (InetSocketAddress) context.channel().remoteAddress();

        pipeline.addFirst(
            "ssl",
            channelProvider.createServerHandler(
                false,
                handshakeTimeout,
                handshakeTimeoutUnit,
                HostAndPort.create(remote.getHostString(), remote.getPort())
            )
        );
        final var handshake = context.channel().newPromise();
        pipeline.addAfter("ssl", "handshaker", new SslHandshakeCompletionHandler(handshake));
        handshake.addListener(f -> {
            if (f.isSuccess()) {
                onHandshakeSuccess.run();
            } else {
                socket.close();
            }
        });
    }
}
