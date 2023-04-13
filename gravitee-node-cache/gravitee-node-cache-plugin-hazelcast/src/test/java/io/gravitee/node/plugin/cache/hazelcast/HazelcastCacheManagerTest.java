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

import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.gravitee.node.api.cache.Cache;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class HazelcastCacheManagerTest {

    private HazelcastCacheManager cut;

    static HazelcastInstance hazelcastInstance;

    @BeforeAll
    public static void beforeAll() {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
    }

    @AfterAll
    public static void afterAll() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    @BeforeEach
    public void beforeEach() {
        cut = new HazelcastCacheManager(hazelcastInstance);
    }

    @Test
    void should_create_new_cache() {
        Cache<String, String> cache = cut.getOrCreateCache("cache");
        assertThat(cache).isNotNull();
    }

    @Test
    void should_not_recreate_new_cache() {
        Cache<String, String> cache = cut.getOrCreateCache("cache");
        assertThat(cache).isNotNull();
        Cache<String, String> cache2 = cut.getOrCreateCache("cache");
        assertThat(cache2).isNotNull();
        assertThat(cache).isSameAs(cache2);
    }

    @Test
    void should_destroy_cache() {
        Cache<String, String> cache = cut.getOrCreateCache("cache");
        assertThat(cache).isNotNull();
        cut.destroy("cache");
        Cache<String, String> cache2 = cut.getOrCreateCache("cache");
        assertThat(cache2).isNotNull();
        assertThat(cache).isNotSameAs(cache2);
    }
}
