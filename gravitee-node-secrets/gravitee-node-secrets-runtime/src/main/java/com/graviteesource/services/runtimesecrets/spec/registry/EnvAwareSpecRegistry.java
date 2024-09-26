package com.graviteesource.services.runtimesecrets.spec.registry;

import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvAwareSpecRegistry {

    private final Map<String, SpecRegistry> registries = new HashMap<>();

    public void register(String envId, Spec spec) {
        registry(envId).register(spec);
    }

    public void unregister(String envId, Spec spec) {
        registry(envId).unregister(spec);
    }

    public Spec getFromName(String envId, String name) {
        return registry(envId).getFromName(name);
    }

    public Spec getFromUri(String envId, String uri) {
        return registry(envId).getFromUri(uri);
    }

    public Spec getFromUriAndKey(String envId, String uriAndKey) {
        return registry(envId).getFromUriAndKey(uriAndKey);
    }

    public Spec getFromID(String envId, String id) {
        return registry(envId).getFromID(id);
    }

    public Spec fromSpec(String envId, Spec query) {
        return registry(envId).fromSpec(query);
    }

    public Spec fromRef(String envId, Ref query) {
        return registry(envId).fromRef(query);
    }

    private SpecRegistry registry(String envId) {
        return registries.computeIfAbsent(envId, ignore -> new SpecRegistry());
    }
}
