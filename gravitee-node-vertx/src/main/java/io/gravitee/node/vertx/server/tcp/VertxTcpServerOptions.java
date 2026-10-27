package io.gravitee.node.vertx.server.tcp;

import io.gravitee.node.vertx.server.VertxServerOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.TrustOptions;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Getter
@SuperBuilder
public class VertxTcpServerOptions extends VertxServerOptions {

    public static final String TCP_PREFIX = "tcp";

    /**
     * Maximum number of client connections held at the same time, shared by every listener of the server.
     * A new connection past this number is closed as soon as it is accepted, before any TLS byte. Connections
     * from the loopback addresses are neither counted nor refused unless {@link #exemptLoopback} is turned off.
     * {@code 0} (the default) means no limit; a negative value is rejected when the server is created, so that a
     * miscalculated limit cannot silently remove the protection.
     * <p>
     * Only set by the caller: no configuration key is read for it.
     */
    protected int maxConnections;

    /**
     * Whether connections from the literal loopback addresses ({@code 127.0.0.1}, {@code ::1}) bypass the limit.
     * They are the pod's own probes, so this is {@code true} by default. Turn it off when something local (a TLS
     * terminator, a sidecar) relays client traffic over loopback: it would otherwise exempt every client.
     * Only meaningful when {@link #maxConnections} is set.
     */
    @Builder.Default
    protected boolean exemptLoopback = true;

    public NetServerOptions createNetServerOptions(KeyCertOptions vertxKeyCertOptions, TrustOptions vertxTrustOptions) {
        var options = new NetServerOptions();

        // Binding port
        options.setPort(this.port);
        options.setHost(this.host);

        if (this.secured) {
            options.setSni(this.sni);
            options.setClientAuth(ClientAuth.valueOf(clientAuth));
        }

        setupTcp(options, vertxKeyCertOptions, vertxTrustOptions);

        if (haProxyProtocol) {
            options.setUseProxyProtocol(true).setProxyProtocolTimeout(haProxyProtocolTimeout);
        }

        return options;
    }
}
