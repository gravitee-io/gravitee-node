package io.gravitee.node.vertx.client.tcp;

import lombok.Builder;
import lombok.Data;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder
public class VertxTcpTarget {

    private String host;
    private int port;

    @Builder.Default
    private boolean secured = false;

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
