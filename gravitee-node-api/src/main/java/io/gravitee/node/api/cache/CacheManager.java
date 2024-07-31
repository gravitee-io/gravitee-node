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
package io.gravitee.node.api.cache;

import io.gravitee.common.service.Service;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CacheManager extends Service<CacheManager> {
    <K, V> Cache<K, V> getOrCreateCache(String name);

    default <K, V, MV> Cache<K, V> getOrCreateCache(String name, ValueMapper<V, MV> valueMapper) {
        return getOrCreateCache(name);
    }

    <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfiguration configuration);

    default <K, V, MV> Cache<K, V> getOrCreateCache(String name, CacheConfiguration configuration, ValueMapper<V, MV> valueMapper) {
        return getOrCreateCache(name, configuration);
    }

    void destroy(String name);
}
