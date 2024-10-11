package io.gravitee.node.api.secrets.runtime.storage;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Cache {
    CacheKey put(CacheKey cacheKey, Entry value);

    Optional<Entry> get(CacheKey cacheKey);

    void computeIfAbsent(CacheKey cacheKey, Supplier<Entry> supplier);

    void evict(CacheKey cacheKey);
}
