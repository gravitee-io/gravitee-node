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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Handler;
import io.vertx.core.internal.net.NetSocketInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.rxjava3.core.net.NetServer;
import io.vertx.rxjava3.core.net.NetSocket;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A {@link NetServer} that closes new connections as soon as they are accepted when its {@link ConnectionLimiter}
 * has no free slot, before any byte is read from them (so before any TLS handshake).
 * <p>
 * It wraps the connect handler the application registers, because the application, not this class, calls
 * {@link #connectHandler(Handler)}. When a {@link SynchronousTlsInstaller} is given, the underlying server
 * accepts plain sockets and the TLS handshake is started here, only for connections that were admitted.
 */
final class LimitedNetServer extends NetServer {

    private final ConnectionLimiter limiter;
    private final Predicate<SocketAddress> exempt;
    private final SynchronousTlsInstaller tls;

    LimitedNetServer(
        io.vertx.core.net.NetServer delegate,
        ConnectionLimiter limiter,
        Predicate<SocketAddress> exempt,
        SynchronousTlsInstaller tls
    ) {
        super(delegate);
        this.limiter = limiter;
        this.exempt = exempt;
        this.tls = tls;
    }

    /**
     * The inherited equality only compares the wrapped delegate; two wrappers around it with different limiters
     * are different servers, so a wrapper is only equal to itself.
     */
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public NetServer connectHandler(Handler<NetSocket> handler) {
        getDelegate().connectHandler(socket -> accept(socket, handler));
        return this;
    }

    private void accept(io.vertx.core.net.NetSocket socket, Handler<NetSocket> handler) {
        final NetSocketInternal internal = (NetSocketInternal) socket;

        // The real peer: with the PROXY protocol, remoteAddress() is the proxied client.
        if (!exempt.test(socket.remoteAddress(true))) {
            if (!limiter.tryAcquire()) {
                socket.close();
                return;
            }
            // The closeHandler belongs to the application; the channel close future fires exactly once, whoever closes.
            internal
                .channelHandlerContext()
                .channel()
                .closeFuture()
                .addListener(f -> limiter.release());
        }

        if (tls == null) {
            handler.handle(NetSocket.newInstance(socket));
        } else {
            tls.install(internal, () -> handler.handle(NetSocket.newInstance(socket)));
        }
    }

    // The SSL context must be resolved before the first connection can be accepted.

    private Single<NetServer> afterTlsReady(Supplier<Single<NetServer>> listen) {
        final Completable ready = tls == null ? Completable.complete() : Completable.fromCompletionStage(tls.ready().toCompletionStage());
        return ready.andThen(Single.defer(listen::get));
    }

    @Override
    public Single<NetServer> listen() {
        return rxListen();
    }

    @Override
    public Single<NetServer> rxListen() {
        return afterTlsReady(super::rxListen);
    }

    @Override
    public Single<NetServer> listen(int port, String host) {
        return rxListen(port, host);
    }

    @Override
    public Single<NetServer> rxListen(int port, String host) {
        return afterTlsReady(() -> super.rxListen(port, host));
    }

    @Override
    public Single<NetServer> listen(int port) {
        return rxListen(port);
    }

    @Override
    public Single<NetServer> rxListen(int port) {
        return afterTlsReady(() -> super.rxListen(port));
    }

    @Override
    public Single<NetServer> listen(SocketAddress localAddress) {
        return rxListen(localAddress);
    }

    @Override
    public Single<NetServer> rxListen(SocketAddress localAddress) {
        return afterTlsReady(() -> super.rxListen(localAddress));
    }
}
