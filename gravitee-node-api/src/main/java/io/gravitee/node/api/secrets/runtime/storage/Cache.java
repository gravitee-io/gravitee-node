package io.gravitee.node.api.secrets.runtime.storage;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Cache {
    CacheKey put(String envId, String naturalId, Entry value);

    Optional<Entry> get(String envId, String naturalId);

    void computeIfAbsent(String envId, String naturalId, Supplier<Entry> supplier);

    void evict(String envId, String naturalId);

    record CacheKey(String envId, String naturalId) {
        @Override
        public String toString() {
            return envId + "-" + naturalId;
        }
    }
}
