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
package io.gravitee.node.plugin.cache.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.plugin.cache.common.AbstractCacheContractTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * @author GraviteeSource Team
 */
class HazelcastCacheContractTest extends AbstractCacheContractTest {

    private static HazelcastInstance hazelcastInstance;
    private static HazelcastCacheManager cacheManager;

    @BeforeAll
    static void startHazelcast() {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
        cacheManager = new HazelcastCacheManager(hazelcastInstance);
    }

    @AfterAll
    static void stopHazelcast() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    @Override
    protected Cache<String, String> createCache(String name, CacheConfiguration configuration) {
        // A non-distributed configuration would give an in-memory cache instead of a Hazelcast one.
        return cacheManager.getOrCreateCache(
            name,
            CacheConfiguration.builder()
                .distributed(true)
                .maxSize(configuration.getMaxSize())
                .timeToLiveInMs(configuration.getTimeToLiveInMs())
                .timeToIdleInMs(configuration.getTimeToIdleInMs())
                .build()
        );
    }
}
