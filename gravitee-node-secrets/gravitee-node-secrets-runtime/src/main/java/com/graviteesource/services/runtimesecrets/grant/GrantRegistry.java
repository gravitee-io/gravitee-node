package com.graviteesource.services.runtimesecrets.grant;

import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrantRegistry {

    private final Map<String, Void> grants = new ConcurrentHashMap<>();

    public void register(DiscoveryContext context) {
        grants.put(context.id().toString(), null);
    }

    public void unregister(DiscoveryContext... contexts) {
        if (contexts != null) {
            Arrays.stream(contexts).map(DiscoveryContext::id).map(UUID::toString).forEach(grants::remove);
        }
    }

    public boolean exists(String token) {
        return grants.containsKey(token);
    }
}
