package io.gravitee.node.vertx.server.tcp;

import io.gravitee.node.vertx.server.VertxServerOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.TrustOptions;
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
