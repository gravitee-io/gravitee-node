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

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheListener;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class HazelcastCache<K, V> implements Cache<K, V> {

    private IMap<K, V> cache;

    private long timeToLiveInMs = -1;

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
        if (timeToLiveInMs > 0) {
            return this.cache.put(key, value, timeToLiveInMs, TimeUnit.MILLISECONDS);
        } else {
            return this.cache.put(key, value);
        }
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit ttlUnit) {
        if (timeToLiveInMs > 0 && timeToLiveInMs < TimeUnit.MILLISECONDS.convert(ttl, ttlUnit)) {
            throw new IllegalArgumentException("Single TTL can't be bigger than TTL defined in the configuration");
        }

        return this.cache.put(key, value, ttl, ttlUnit);
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
    public String addCacheListener(CacheListener<K, V> cacheListener) {
        UUID id =
            this.cache.addEntryListener(
                    new MapListenerAdapter<K, V>() {
                        @Override
                        public void onEntryEvent(EntryEvent<K, V> event) {
                            switch (event.getEventType()) {
                                case ADDED:
                                    cacheListener.onEntryAdded(event.getKey(), event.getValue());
                                    break;
                                case REMOVED:
                                case EVICTED:
                                    cacheListener.onEntryEvicted(event.getKey(), event.getOldValue());
                                    break;
                                case UPDATED:
                                    cacheListener.onEntryUpdated(event.getKey(), event.getOldValue(), event.getValue());
                                    break;
                                case EXPIRED:
                                    cacheListener.onEntryExpired(event.getKey(), event.getValue());
                                    break;
                                default:
                                case EVICT_ALL:
                                case CLEAR_ALL:
                                case MERGED:
                                case INVALIDATION:
                                case LOADED:
                                    break;
                            }
                        }
                    },
                    true
                );

        return id.toString();
    }

    @Override
    public boolean removeCacheListener(final String cacheListenerId) {
        if (cacheListenerId != null) {
            return this.cache.removeEntryListener(UUID.fromString(cacheListenerId));
        }
        return false;
    }
}
