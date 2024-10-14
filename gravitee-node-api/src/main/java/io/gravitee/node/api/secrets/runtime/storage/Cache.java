package io.gravitee.node.api.secrets.runtime.storage;

import io.gravitee.node.api.secrets.model.Secret;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Cache {
    void put(CacheKey cacheKey, Entry value);

    void putPartial(CacheKey cacheKey, Map<String, Secret> partial);

    Optional<Entry> get(CacheKey cacheKey);

    void computeIfAbsent(CacheKey cacheKey, Supplier<Entry> supplier);

    void evict(CacheKey cacheKey);
}
