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
package io.gravitee.node.api.cluster;

/**
 * SPI for consumers needing a {@link DistributedMap}. Deliberately independent of
 * {@link ClusterManager} so that distributed-map backing (e.g. Hazelcast) can be active
 * without requiring the same implementation to also back cluster membership and
 * messaging — or vice versa.
 *
 * <p>Implementations are responsible for the lifecycle of the underlying storage
 * (e.g. booting a Hazelcast instance, connecting to an external store). Consumers
 * autowire the provider and request maps by name.</p>
 *
 * <p>Map names share a single backing namespace within the JVM: two callers asking
 * for the same {@code name} will read and write the same entries. Callers that need
 * isolation should prefix the name (e.g. {@code "my-plugin:counters"}).</p>
 */
public interface DistributedMapProvider {
    /**
     * Returns (or lazily creates) the {@link DistributedMap} with the given {@code name}.
     *
     * @param name the name used to retrieve (or lazily create) the distributed map
     * @param <K> the key type
     * @param <V> the value type
     * @return a {@link DistributedMap}
     */
    <K, V> DistributedMap<K, V> get(String name);
}
