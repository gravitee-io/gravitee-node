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

    public static String TCP_PREFIX = "tcp";
    private boolean logActivity;

    public NetServerOptions createNetServerOptions() {
        var options = new NetServerOptions();

        // Binding port
        options.setPort(this.port);
        options.setHost(this.host);

        options.setLogActivity(this.logActivity);

        // FIXME: must be secure with SNI on
        if (this.secured) {
            options.setSni(sni);
            // Specify client auth (mtls).
            options.setClientAuth(ClientAuth.valueOf(clientAuth));
        }

        setupTcp(options);

        if (haProxyProtocol) {
            options.setUseProxyProtocol(true).setProxyProtocolTimeout(haProxyProtocolTimeout);
        }

        return options;
    }
}
