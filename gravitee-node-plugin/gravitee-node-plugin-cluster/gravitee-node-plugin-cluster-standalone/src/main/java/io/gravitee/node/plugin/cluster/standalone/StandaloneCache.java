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
package io.gravitee.node.plugin.cluster.standalone;

import com.google.common.base.Verify;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import io.gravitee.node.api.cache.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneCache<K, V> implements Cache<K, V> {

    private final String name;
    private final CacheConfiguration configuration;
    private final AtomicInteger counter = new AtomicInteger();
    private final com.google.common.cache.Cache<K, ValueWrapper<V>> internalCache;
    private final Set<CacheListener<K, V>> cacheListeners = new HashSet<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "gio-standalone-cache"));

    public StandaloneCache(String name, CacheConfiguration configuration) {
        this.name = name;
        this.configuration = configuration;

        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (configuration.getMaxSize() > 0) {
            cacheBuilder.maximumSize(configuration.getMaxSize());
        }
        if (configuration.getTimeToIdleSeconds() > 0) {
            cacheBuilder.expireAfterAccess(configuration.getTimeToIdleSeconds(), TimeUnit.SECONDS);
        }
        if (configuration.getTimeToLiveSeconds() > 0) {
            cacheBuilder.expireAfterWrite(configuration.getTimeToLiveSeconds(), TimeUnit.SECONDS);
        }

        cacheBuilder.removalListener((RemovalListener<K, V>) notification -> counter.decrementAndGet());
        internalCache = cacheBuilder.build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return counter.get();
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public Collection<V> values() {
        return this.internalCache.asMap()
            .values()
            .stream()
            .filter(e -> e.expirationTimeMillis == 0 || System.currentTimeMillis() <= e.expirationTimeMillis)
            .map(vValueWrapper -> vValueWrapper.value)
            .collect(Collectors.toList());
    }

    @Override
    public V get(K key) {
        ValueWrapper<V> vw = internalCache.getIfPresent(key);
        // If value is present and ttl not expired then return it otherwise invalidates cache and return null
        if (vw != null && (vw.expirationTimeMillis == 0 || System.currentTimeMillis() <= vw.expirationTimeMillis)) {
            return vw.value;
        }
        this.internalCache.invalidate(key);
        return null;
    }

    @Override
    public V put(K key, V value) {
        return this.put(key, value, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit ttlUnit) {
        if (this.configuration.getTimeToLiveSeconds() > 0) {
            Verify.verify(
                TimeUnit.MILLISECONDS.convert(ttl, ttlUnit) <=
                TimeUnit.MILLISECONDS.convert(this.configuration.getTimeToLiveSeconds(), TimeUnit.SECONDS),
                "single ttl can't be bigger than ttl defined in the configuration"
            );
        }

        V currentValue = this.get(key);
        long ttlMillis = TimeUnit.MILLISECONDS.convert(ttl, ttlUnit);
        long expirationTimeMillis = ttlMillis != 0 ? System.currentTimeMillis() + ttlMillis : 0;
        counter.incrementAndGet();
        this.internalCache.put(key, new ValueWrapper<>(value, expirationTimeMillis));

        executorService.execute(() -> {
            if (currentValue == null) {
                cacheListeners.forEach(listener -> new EntryEvent<>(name, EntryEventType.ADDED, key, null, value));
            } else {
                cacheListeners.forEach(listener -> new EntryEvent<>(name, EntryEventType.UPDATED, key, currentValue, value));
            }
        });

        return currentValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Map<K, ValueWrapper<V>> wrapperMap = m
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new ValueWrapper<>(e.getValue(), 0)));

        counter.addAndGet(wrapperMap.size());
        this.internalCache.putAll(wrapperMap);
    }

    @Override
    public V evict(K key) {
        V v = this.get(key);
        this.internalCache.invalidate(key);

        executorService.execute(() -> cacheListeners.forEach(cacheListener -> new EntryEvent<>(name, EntryEventType.REMOVED, key, v, null))
        );

        return v;
    }

    @Override
    public void clear() {
        this.internalCache.invalidateAll();
    }

    @Override
    public void addCacheListener(CacheListener<K, V> listener) {
        cacheListeners.add(listener);
    }

    @Override
    public boolean removeCacheListener(CacheListener<K, V> listener) {
        return cacheListeners.remove(listener);
    }

    private static class ValueWrapper<T> {

        private final T value;
        private final long expirationTimeMillis;

        public ValueWrapper(T value, long expirationTimeMillis) {
            this.value = value;

            this.expirationTimeMillis = expirationTimeMillis;
        }
    }
}
