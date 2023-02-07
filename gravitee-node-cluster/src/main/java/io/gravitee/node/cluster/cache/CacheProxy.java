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
package io.gravitee.node.cluster.cache;

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CacheProxy<K, V> implements Cache<K, V> {

    private final String name;
    private final CacheConfiguration configuration;

    private boolean started = false;

    private Cache<K, V> target;

    private final List<CacheListener<K, V>> listeners = new ArrayList<>();

    public CacheProxy(String name, CacheConfiguration configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return name;
    }

    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public int size() {
        if (started) {
            return target.size();
        }

        return 0;
    }

    @Override
    public boolean isEmpty() {
        if (started) {
            return target.isEmpty();
        }

        return true;
    }

    @Override
    public Collection<V> values() {
        checkState();
        return target.values();
    }

    @Override
    public V get(K key) {
        checkState();
        return target.get(key);
    }

    @Override
    public V put(K key, V value) {
        checkState();
        return target.put(key, value);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit ttlUnit) {
        checkState();
        return target.put(key, value, ttl, ttlUnit);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkState();
        target.putAll(m);
    }

    @Override
    public V evict(K key) {
        checkState();
        return target.evict(key);
    }

    @Override
    public void clear() {
        checkState();
        target.clear();
    }

    @Override
    public void addCacheListener(CacheListener<K, V> listener) {
        if (!started) {
            listeners.add(listener);
        } else {
            target.addCacheListener(listener);
        }
    }

    @Override
    public boolean removeCacheListener(CacheListener<K, V> listener) {
        if (started) {
            return target.removeCacheListener(listener);
        }

        return true;
    }

    private void checkState() {
        if (!started) {
            throw new IllegalStateException("Cache " + name + " is not yet loaded");
        }
    }

    public void setTarget(Cache<K, V> target) {
        this.target = target;

        listeners.forEach(target::addCacheListener);
        listeners.clear();

        this.started = true;
    }
}
