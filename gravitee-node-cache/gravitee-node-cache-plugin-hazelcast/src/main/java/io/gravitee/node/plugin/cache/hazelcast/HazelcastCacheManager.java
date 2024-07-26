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
package io.gravitee.node.plugin.cache.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.plugin.cache.common.InMemoryCache;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class HazelcastCacheManager extends AbstractService<CacheManager> implements CacheManager {

    private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    private final HazelcastInstance hazelcastInstance;

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(final String name) {
        return getOrCreateCache(name, new CacheConfiguration());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Cache<K, V> getOrCreateCache(final String name, final CacheConfiguration configuration) {
        return (Cache<K, V>) caches.computeIfAbsent(
            name,
            s -> {
                if (configuration.isDistributed()) {
                    // First, configure the cache using Hazelcast config
                    configureCache(s, configuration);

                    // Then create the cache entity
                    return new HazelcastCache<>(hazelcastInstance.getMap(name), configuration.getTimeToLiveInMs());
                } else {
                    return new InMemoryCache<>(name, configuration);
                }
            }
        );
    }

    @Override
    public void destroy(final String cacheName) {
        Cache<?, ?> cache = caches.remove(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void configureCache(String name, CacheConfiguration configuration) {
        Config config = hazelcastInstance.getConfig();

        if (!config.getMapConfigs().containsKey(name)) {
            MapConfig mapConfig = new MapConfig(name);

            if (configuration.getMaxSize() > 0) {
                mapConfig.getEvictionConfig().setSize((int) configuration.getMaxSize());
                if (mapConfig.getEvictionConfig().getEvictionPolicy().equals(EvictionPolicy.NONE)) {
                    // Set "Least Recently Used" eviction policy if not have eviction configured
                    mapConfig.getEvictionConfig().setEvictionPolicy(EvictionPolicy.LRU);
                }
            }

            if (configuration.getTimeToIdleInMs() > 0) {
                mapConfig.setMaxIdleSeconds((int) TimeUnit.SECONDS.convert(configuration.getTimeToIdleInMs(), TimeUnit.MILLISECONDS));
            }

            if (configuration.getTimeToLiveInMs() > 0) {
                mapConfig.setTimeToLiveSeconds((int) TimeUnit.SECONDS.convert(configuration.getTimeToLiveInMs(), TimeUnit.MILLISECONDS));
            }

            config.addMapConfig(mapConfig);
        }
    }
}
