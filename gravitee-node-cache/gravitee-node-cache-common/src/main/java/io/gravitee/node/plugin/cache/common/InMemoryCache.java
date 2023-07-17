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
package io.gravitee.node.plugin.cache.common;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class InMemoryCache<K, V> implements Cache<K, V> {

    private final String name;
    private final CacheConfiguration configuration;
    private final com.github.benmanes.caffeine.cache.Cache<K, ExpiringValue<V>> internalCache;
    private final Map<String, CacheListener<K, V>> cacheListeners = new HashMap<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "gio-cache-in-memory-listeners")
    );

    public InMemoryCache(final String name, final CacheConfiguration configuration) {
        this.name = name;
        this.configuration = configuration;

        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
        cacheBuilder.executor(executorService);
        if (configuration.getMaxSize() > 0) {
            cacheBuilder.maximumSize(configuration.getMaxSize());
        }
        if (configuration.getTimeToIdleInMs() > 0) {
            cacheBuilder.expireAfterAccess(configuration.getTimeToIdleInMs(), TimeUnit.MILLISECONDS);
        }
        if (configuration.getTimeToLiveInMs() > 0) {
            cacheBuilder.expireAfterWrite(configuration.getTimeToLiveInMs(), TimeUnit.MILLISECONDS);
        }
        cacheBuilder.removalListener((k, v, cause) ->
            cacheListeners.forEach((id, listener) -> {
                try {
                    K typeKey = (K) k;
                    ExpiringValue<V> expiringValue = (ExpiringValue<V>) v;
                    V value = expiringValue != null ? expiringValue.value : null;
                    switch (cause) {
                        case EXPLICIT:
                            if (expiringValue != null && expiringValue.hasExpired()) {
                                listener.onEntryExpired(typeKey, value);
                            } else {
                                listener.onEntryEvicted(typeKey, value);
                            }
                            break;
                        case EXPIRED:
                            listener.onEntryExpired(typeKey, value);
                            break;
                        case SIZE:
                            listener.onEntryEvicted(typeKey, value);
                            break;
                        case REPLACED:
                        case COLLECTED:
                        default:
                            break;
                    }
                } catch (Exception e) {
                    log.error("Unable to trigger cache listener");
                }
            })
        );
        internalCache = cacheBuilder.build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return (int) internalCache.estimatedSize();
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public Collection<V> values() {
        return this.internalCache.asMap()
            .entrySet()
            .stream()
            .map(entry -> {
                K key = entry.getKey();
                ExpiringValue<V> expiringValue = entry.getValue();
                if (expiringValue.hasExpired()) {
                    internalCache.invalidate(key);
                    return null;
                }
                return expiringValue.value;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public V get(final K key) {
        ExpiringValue<V> expiringValue = internalCache.getIfPresent(key);
        // If value is present and expired invalidates cache and return null, otherwise return value
        if (expiringValue != null) {
            if (expiringValue.hasExpired()) {
                internalCache.invalidate(key);
                return null;
            }
            return expiringValue.value;
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        return this.put(key, value, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit ttlUnit) {
        long ttlMillis = TimeUnit.MILLISECONDS.convert(ttl, ttlUnit);
        if (this.configuration.getTimeToLiveInMs() > 0 && this.configuration.getTimeToLiveInMs() < ttlMillis) {
            throw new IllegalArgumentException("TTL can't be bigger than ttl defined in the cache configuration");
        }

        V oldValue = get(key);
        long expirationTimeMillis = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0;
        this.internalCache.put(key, new ExpiringValue<>(value, expirationTimeMillis));

        executorService.execute(() ->
            cacheListeners.forEach((id, listener) -> {
                if (oldValue == null) {
                    listener.onEntryAdded(key, value);
                } else {
                    listener.onEntryUpdated(key, oldValue, value);
                }
            })
        );

        return oldValue;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> values) {
        Map<K, ExpiringValue<V>> expiringValueMap = values
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new ExpiringValue<>(e.getValue(), 0)));

        this.internalCache.putAll(expiringValueMap);
    }

    @Override
    public V evict(final K key) {
        V currentValue = this.get(key);
        this.internalCache.invalidate(key);
        return currentValue;
    }

    @Override
    public void clear() {
        this.internalCache.invalidateAll();
    }

    @Override
    public String addCacheListener(final CacheListener<K, V> cacheListener) {
        String listenerCacheId = io.gravitee.common.utils.UUID.random().toString();
        cacheListeners.put(listenerCacheId, cacheListener);

        return listenerCacheId;
    }

    @Override
    public boolean removeCacheListener(final String listenerCacheId) {
        return cacheListeners.remove(listenerCacheId) != null;
    }

    @RequiredArgsConstructor
    private static class ExpiringValue<T> {

        private final T value;
        private final long expirationTimeMillis;

        public boolean hasExpired() {
            return expirationTimeMillis > 0 && expirationTimeMillis <= System.currentTimeMillis();
        }
    }
}
