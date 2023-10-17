package io.gravitee.node.vertx.server.tcp;

import io.gravitee.node.vertx.server.VertxServerOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.NetServerOptions;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Getter
@SuperBuilder
public class VertxTcpServerOptions extends VertxServerOptions {

    public static final String TCP_PREFIX = "tcp";

    public NetServerOptions createNetServerOptions() {
        var options = new NetServerOptions();

        // Binding port
        options.setPort(this.port);
        options.setHost(this.host);

        if (this.secured && this.sni) {
            options.setSni(true);
            options.setClientAuth(ClientAuth.valueOf(clientAuth));
        } else {
            throw new IllegalArgumentException("Cannot start unsecured TCP server without SNI enabled");
        }

        setupTcp(options);

        if (haProxyProtocol) {
            options.setUseProxyProtocol(true).setProxyProtocolTimeout(haProxyProtocolTimeout);
        }

        return options;
    }
}
