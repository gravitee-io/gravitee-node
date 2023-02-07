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
import io.gravitee.node.api.cache.CacheManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CacheManagerDelegate implements CacheManager {

    private CacheManager target;

    private final List<CacheProxy<?, ?>> caches = new ArrayList<>();

    private final CacheConfiguration defaultCacheConfiguration = new CacheConfiguration();

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name) {
        return getOrCreateCache(name, defaultCacheConfiguration);
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfiguration configuration) {
        return getOrCreateCache0(name, configuration);
    }

    @Override
    public void destroy(String name) {
        target.destroy(name);
    }

    private <K, V> Cache<K, V> getOrCreateCache0(String name, CacheConfiguration configuration) {
        CacheProxy<K, V> cacheProxy = new CacheProxy<>(name, configuration);

        if (target != null) {
            target.getOrCreateCache(name, configuration);
        }

        caches.add(cacheProxy);

        return cacheProxy;
    }

    public void setTarget(CacheManager target) {
        this.target = target;

        // When the cluster plugin is deployed, reload existing caches
        caches.forEach(cacheProxy -> cacheProxy.setTarget(target.getOrCreateCache(cacheProxy.getName(), cacheProxy.getConfiguration())));

        caches.clear();
    }
}
