package io.gravitee.node.api.secrets.runtime.storage;

import io.gravitee.node.api.secrets.runtime.spec.Spec;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record CacheKey(String envId, String uri) implements Comparable<CacheKey> {
    @Override
    public String toString() {
        return envId + "-" + uri;
    }

    public static CacheKey from(Spec spec) {
        return new CacheKey(spec.envId(), spec.uri());
    }

    @Override
    public int compareTo(CacheKey o) {
        return this.toString().compareTo(o.toString());
    }
}
