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
package io.gravitee.node.plugin.cluster.hazelcast.provider;

import com.hazelcast.map.IMap;
import io.gravitee.node.api.cluster.DistributedMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HazelcastDistributedMap<K, V> implements DistributedMap<K, V> {

    private final IMap<K, V> delegate;

    @Override
    public V get(K key) {
        return delegate.get(key);
    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        delegate.set(key, value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void lock(K key) {
        delegate.lock(key);
    }

    @Override
    public void unlock(K key) {
        delegate.unlock(key);
    }
}
