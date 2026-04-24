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
 * A cluster-wide key-value store with per-entry TTL and per-key locking.
 *
 * <p>Obtained via {@link ClusterManager#distributedMap(String)}. In clustered deployments
 * (e.g. the Hazelcast cluster plugin), state is shared across members. In standalone
 * deployments, state is local to the single node.</p>
 */
public interface DistributedMap<K, V> {
    V get(K key);

    /**
     * Stores the value under {@code key} and expires it after {@code ttlMillis}.
     * A value of {@code 0} means the entry never expires.
     */
    void put(K key, V value, long ttlMillis);

    /**
     * Acquires a cluster-wide lock scoped to {@code key}. Blocks until the lock is available.
     */
    void lock(K key);

    /**
     * Releases the lock previously acquired via {@link #lock(Object)}.
     */
    void unlock(K key);
}
