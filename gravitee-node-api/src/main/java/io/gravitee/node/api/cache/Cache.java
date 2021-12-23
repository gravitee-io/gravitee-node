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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Cache<K, V> {
  String getName();

  int size();

  boolean isEmpty();

  Collection<V> values();

  V get(K key);

  V put(K key, V value);

  V put(K key, V value, long ttl, TimeUnit ttlUnit);

  void putAll(Map<? extends K, ? extends V> m);

  V evict(K key);

  void clear();

  void addCacheListener(CacheListener<K, V> listener);

  boolean removeCacheListener(CacheListener<K, V> listener);
}
