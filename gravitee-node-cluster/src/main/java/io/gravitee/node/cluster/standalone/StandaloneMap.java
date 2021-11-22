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
package io.gravitee.node.cluster.standalone;

import io.gravitee.node.api.cache.GMap;
import io.gravitee.node.api.cache.MapListener;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneMap<K, V>
  extends ConcurrentHashMap<K, V>
  implements GMap<K, V> {

  private final String name;
  private Map<UUID, MapListener<K, V>> mapListeners = new ConcurrentHashMap<>();

  public StandaloneMap(String name) {
    this.name = name;
  }

  @Override
  public UUID addMapListener(
    MapListener<K, V> mapListener,
    boolean includeValue
  ) {
    UUID uuid = io.gravitee.common.utils.UUID.random();
    mapListeners.put(uuid, mapListener);

    return uuid;
  }

  @Override
  public boolean removeMapListener(UUID id) {
    if (!mapListeners.containsKey(id)) {
      return false;
    } else {
      mapListeners.remove(id);
      return true;
    }
  }
}
