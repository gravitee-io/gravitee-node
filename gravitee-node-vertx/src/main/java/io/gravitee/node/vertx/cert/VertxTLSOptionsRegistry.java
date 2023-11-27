package io.gravitee.node.vertx.cert;

import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxTLSOptionsRegistry {

    private final Map<String, KeyCertOptions> keyCertOptionsRegistry;
    private final Map<String, TrustOptions> trustOptionsRegistry;

    public VertxTLSOptionsRegistry() {
        this.keyCertOptionsRegistry = new HashMap<>();
        this.trustOptionsRegistry = new HashMap<>();
    }

    public void registerOptions(String serverId, @Nonnull KeyCertOptions options) {
        Objects.requireNonNull(options, "options are mandatory");
        this.keyCertOptionsRegistry.putIfAbsent(serverId, options);
    }

    public void registerOptions(String serverId, @Nonnull TrustOptions options) {
        Objects.requireNonNull(options, "options are mandatory");
        this.trustOptionsRegistry.putIfAbsent(serverId, options);
    }

    public KeyCertOptions lookupKeyCertOptions(@Nonnull String serverId) {
        return keyCertOptionsRegistry.get(serverId);
    }

    public TrustOptions lookupTrustOptions(@Nonnull String serverId) {
        return trustOptionsRegistry.get(serverId);
    }
}
