/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.plugin.cache.redis;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.api.cache.ValueMapper;
import io.gravitee.node.plugin.cache.redis.configuration.RedisConfiguration;
import io.gravitee.node.plugin.cache.redis.configuration.RedisOptionsFactory;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisCacheManager extends AbstractService<CacheManager> implements CacheManager {

    private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    @Autowired
    @Setter(AccessLevel.PACKAGE)
    private RedisConfiguration redisConfiguration;

    @Autowired
    @Setter(AccessLevel.PACKAGE)
    private Vertx vertx;

    private RedisAPI redisAPI;

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(final String name) {
        return getOrCreateCache(name, new CacheConfiguration(), null);
    }

    @Override
    public <K, V, MV> Cache<K, V> getOrCreateCache(String name, ValueMapper<V, MV> valueMapper) {
        return getOrCreateCache(name, new CacheConfiguration(), valueMapper);
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfiguration configuration) {
        return getOrCreateCache(name, configuration, null);
    }

    @Override
    public <K, V, MV> Cache<K, V> getOrCreateCache(String name, CacheConfiguration configuration, ValueMapper<V, MV> valueMapper) {
        return (Cache<K, V>) caches.computeIfAbsent(
            name,
            s -> new RedisCache<V>(name, configuration, getOrCreateRedisAPI(), (ValueMapper<V, String>) valueMapper)
        );
    }

    @Override
    public void destroy(final String cacheName) {
        Cache<?, ?> cache = caches.remove(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (redisAPI != null) {
            redisAPI.close();
        }
    }

    private synchronized RedisAPI getOrCreateRedisAPI() {
        if (redisAPI == null) {
            this.redisAPI = RedisAPI.api(Redis.createClient(vertx, RedisOptionsFactory.build(redisConfiguration)));
        }
        return this.redisAPI;
    }
}
