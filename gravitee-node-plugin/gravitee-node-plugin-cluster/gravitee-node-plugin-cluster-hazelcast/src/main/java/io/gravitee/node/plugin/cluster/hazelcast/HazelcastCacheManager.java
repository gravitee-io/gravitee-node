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
package io.gravitee.node.plugin.cluster.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    private final HazelcastInstance hazelcastInstance;

    public HazelcastCacheManager(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name) {
        return getOrCreateCache(name, new CacheConfiguration());
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfiguration configuration) {
        return caches.computeIfAbsent(
            name,
            s -> {
                // First, configure the cache using Hazelcast config
                configureCache(s, configuration);

                // Then create the cache entity
                return new HazelcastCache<>(hazelcastInstance.getMap(name), configuration);
            }
        );
    }

    @Override
    public void destroy(String name) {
        Cache cache = caches.remove(name);
        if (cache != null) {
            cache.clear();
        }
    }

    private void configureCache(String name, CacheConfiguration configuration) {
        Config config = hazelcastInstance.getConfig();

        if (config != null && !config.getMapConfigs().containsKey(name)) {
            MapConfig resourceConfig = new MapConfig(name);

            // Cache is standalone, no backup wanted.
            resourceConfig.setAsyncBackupCount(0);
            resourceConfig.setBackupCount(0);

            if (configuration.getMaxSize() > 0) {
                resourceConfig.getEvictionConfig().setSize((int) configuration.getMaxSize());
            }

            if (resourceConfig.getEvictionConfig().getEvictionPolicy().equals(EvictionPolicy.NONE)) {
                // Set "Least Recently Used" eviction policy if not have eviction configured
                resourceConfig.getEvictionConfig().setEvictionPolicy(EvictionPolicy.LRU);
            }

            if (configuration.getTimeToIdleSeconds() > 0) {
                resourceConfig.setMaxIdleSeconds((int) configuration.getTimeToIdleSeconds());
            }

            if (configuration.getTimeToLiveSeconds() > 0) {
                resourceConfig.setTimeToLiveSeconds((int) configuration.getTimeToLiveSeconds());
            }

            config.addMapConfig(resourceConfig);
        }
    }
}
