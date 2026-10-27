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

import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.cert.VertxKeyCertOptions;
import io.gravitee.node.vertx.cert.VertxTrustOptions;
import io.gravitee.node.vertx.server.VertxServer;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetServer;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxTcpServer extends VertxServer<NetServer, VertxTcpServerOptions> {

    public static final String KIND = "tcp";

    private final ConnectionLimiter limiter;
    private volatile Predicate<SocketAddress> exempt;

    public VertxTcpServer(
        Vertx vertx,
        VertxTcpServerOptions options,
        KeyStoreLoaderManager keyStoreLoaderManager,
        TrustStoreLoaderManager trustStoreLoaderManager,
        CRLLoaderManager crlLoaderManager
    ) {
        super(vertx, options, keyStoreLoaderManager, trustStoreLoaderManager, crlLoaderManager);
        if (options != null && options.getMaxConnections() < 0) {
            throw new IllegalArgumentException("maxConnections must not be negative: " + options.getMaxConnections());
        }
        this.exempt = loopbackExemption(options);
        // One count for every NetServer of this server (several listeners share the budget).
        this.limiter = options != null && options.getMaxConnections() > 0 ? new ConnectionLimiter(options.getMaxConnections()) : null;
    }

    private static Predicate<SocketAddress> loopbackExemption(VertxTcpServerOptions options) {
        return options != null && options.isExemptLoopback() ? ConnectionLimiter::isLoopback : address -> false;
    }

    @Override
    public String type() {
        return KIND;
    }

    @Override
    public NetServer newInstance() {
        startKeyStoreManagers();
        final NetServerOptions netServerOptions = options.createNetServerOptions(
            new VertxKeyCertOptions(keyStoreLoaderManager.getKeyManager()),
            new VertxTrustOptions(trustStoreLoaderManager.getCertificateManager())
        );
        final NetServer netServer = limiter == null ? vertx.createNetServer(netServerOptions) : createLimitedNetServer(netServerOptions);
        delegates.add(netServer);
        return netServer;
    }

    private NetServer createLimitedNetServer(NetServerOptions netServerOptions) {
        SynchronousTlsInstaller tls = null;
        if (netServerOptions.isSsl()) {
            // Accept plain: TLS is started by the limited server, only on connections that were admitted.
            tls = new SynchronousTlsInstaller((VertxInternal) vertx.getDelegate(), netServerOptions);
            netServerOptions.setSsl(false);
        }
        return new LimitedNetServer(vertx.getDelegate().createNetServer(netServerOptions), limiter, exempt, tls);
    }

    /**
     * Replaces the predicate of the connections that are neither counted nor refused. Must be called before
     * {@link #newInstance()}. For tests, which connect from the loopback addresses.
     */
    void exemptConnections(Predicate<SocketAddress> exempt) {
        this.exempt = exempt;
    }

    /**
     * @return the number of counted connections currently held, 0 when the server is not limited.
     */
    int currentConnections() {
        return limiter == null ? 0 : limiter.count();
    }

    @Override
    public List<NetServer> instances() {
        return Collections.unmodifiableList(delegates);
    }

    @Override
    public VertxTcpServerOptions options() {
        return options;
    }
}
