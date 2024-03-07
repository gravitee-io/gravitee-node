package io.gravitee.node.vertx.client.tcp;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VertxTcpProxyOptions implements Serializable {

    @Serial
    private static final long serialVersionUID = 6710746676968205250L;

    private boolean enabled;

    private boolean useSystemProxy;

    private String host;

    private int port;

    private String username;

    private String password;

    private VertxTcpProxyType type = VertxTcpProxyType.SOCKS5;
}
