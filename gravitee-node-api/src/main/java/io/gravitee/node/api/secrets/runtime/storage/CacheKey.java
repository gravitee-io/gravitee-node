package io.gravitee.node.api.secrets.runtime.storage;

import io.gravitee.node.api.secrets.runtime.spec.Spec;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record CacheKey(String envId, String naturalId) {
    @Override
    public String toString() {
        return envId + "-" + naturalId;
    }

    public static CacheKey from(Spec spec) {
        return new CacheKey(spec.envId(), spec.naturalId());
    }
}
