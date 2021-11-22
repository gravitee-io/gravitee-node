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
package io.gravitee.node.cluster.hazelcast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import io.gravitee.node.api.cache.EntryEventType;
import io.gravitee.node.api.cache.GMap;
import io.gravitee.node.api.cache.MapListener;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastMap<K, V> implements GMap<K, V> {

  private final IMap<K, V> iMap;

  public HazelcastMap(IMap<K, V> iMap) {
    this.iMap = iMap;
  }

  @Override
  public int size() {
    return iMap.size();
  }

  @Override
  public boolean isEmpty() {
    return iMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return iMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return iMap.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return iMap.get(key);
  }

  @Override
  public V put(K key, V value) {
    return iMap.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return iMap.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    iMap.putAll(m);
  }

  @Override
  public void clear() {
    iMap.clear();
  }

  @Override
  public Set<K> keySet() {
    return iMap.keySet();
  }

  @Override
  public Collection<V> values() {
    return iMap.values();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return iMap.entrySet();
  }

  @Override
  public UUID addMapListener(
    MapListener<K, V> mapListener,
    boolean includeValue
  ) {
    return iMap.addEntryListener(
      new MapListenerAdapter<K, V>() {
        @Override
        public void onEntryEvent(EntryEvent<K, V> event) {
          mapListener.onEntryEvent(
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
      includeValue
    );
  }

  @Override
  public boolean removeMapListener(UUID id) {
    return iMap.removeEntryListener(id);
  }
}
