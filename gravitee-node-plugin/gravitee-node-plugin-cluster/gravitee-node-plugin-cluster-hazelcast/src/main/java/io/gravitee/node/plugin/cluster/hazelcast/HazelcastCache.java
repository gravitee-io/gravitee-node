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

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import io.gravitee.node.api.cache.EntryEventType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastCache<K, V> implements Cache<K, V> {

    private final IMap<K, V> cache;
    private final Map<CacheListener<K, V>, UUID> listeners = new HashMap<>();
    private final long timeToLiveMillis;

    public HazelcastCache(IMap<K, V> cache, CacheConfiguration configuration) {
        this.cache = cache;
        this.timeToLiveMillis = configuration.getTimeToLiveSeconds();
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public int size() {
        return this.cache.size();
    }

    @Override
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }

    @Override
    public Collection<V> values() {
        return this.cache.values();
    }

    @Override
    public V get(K key) {
        return this.cache.get(key);
    }

    @Override
    public V put(K key, V value) {
        return this.cache.put(key, value);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit ttlUnit) {
        long ttlMillis = TimeUnit.MILLISECONDS.convert(ttl, ttlUnit);

        if (timeToLiveMillis > 0 && ttlMillis > timeToLiveMillis) {
            throw new RuntimeException("single ttl can't be bigger than ttl defined in the configuration");
        }

        return this.cache.put(key, value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        this.cache.putAll(m);
    }

    @Override
    public V evict(K key) {
        V v = cache.get(key);
        cache.remove(key);

        return v;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void addCacheListener(CacheListener<K, V> cacheListener) {
        UUID id =
            this.cache.addEntryListener(
                    new MapListenerAdapter<K, V>() {
                        @Override
                        public void onEntryEvent(EntryEvent<K, V> event) {
                            cacheListener.onEvent(
                                new io.gravitee.node.api.cache.EntryEvent<>(
                                    event.getSource(),
                                    EntryEventType.getByType(event.getEventType().getType()),
                                    event.getKey(),
                                    event.getOldValue(),
                                    event.getValue()
                                )
                            );
                        }
                    },
                    true
                );

        listeners.put(cacheListener, id);
    }

    @Override
    public boolean removeCacheListener(CacheListener<K, V> cacheListener) {
        UUID id = listeners.remove(cacheListener);

        if (id != null) {
            return this.cache.removeEntryListener(id);
        }

        return false;
    }
}
