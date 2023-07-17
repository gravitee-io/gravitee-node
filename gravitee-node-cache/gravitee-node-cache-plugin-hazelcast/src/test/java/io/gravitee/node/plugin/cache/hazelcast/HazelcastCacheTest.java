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
import static org.awaitility.Awaitility.await;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheListener;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastCacheTest {

    private static final String CACHE_NAME = "test-cache";
    private static final String TEST_KEY = "key1";
    private static final String TEST_VALUE = "value1";
    private static final String TEST_VALUE_UPDATED = "value1_updated";

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

    @AfterEach
    public void afterEach() {
        IMap<Object, Object> map = hazelcastInstance.getMap(CACHE_NAME);
        map.destroy();
    }

    @Nested
    class GetTest {

        @Test
        void should_return_null_when_getting_non_existing_key() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            assertThat(cache.get("no_key")).isNull();
        }

        @Test
        void should_return_null_when_entry_expired() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS);

            await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_return_no_values_when_no_entry_in_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            assertThat(cache.size()).isZero();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_return_no_values_when_entry_expired() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS);

            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(cache.values()).isEmpty());
        }
    }

    @Nested
    class PutTest {

        @Test
        void should_put_value_to_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            cache.put(TEST_KEY, TEST_VALUE);

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_put_values_to_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE, "key2", "value2"));

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.get("key2")).isNotNull();
            assertThat(cache.values()).hasSize(2);
            assertThat(cache.values()).containsOnly(TEST_VALUE, "value2");
        }

        @Test
        void should_put_value_to_cache_and_expired_after_ttl() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), 1000);
            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);

            await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_put_value_to_cache_with_custom_ttl_and_expired_after_ttl() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);

            await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_putting_new_value_to_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.put(TEST_KEY, TEST_VALUE);
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryUpdated(final String key, final String oldValue, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.put(TEST_KEY, TEST_VALUE);
            cache.put(TEST_KEY, TEST_VALUE_UPDATED);
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }

    @Nested
    class EvictTest {

        @Test
        void should_ignore_evicting_not_existing_entry_from_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);

            assertThat(cache.evict(TEST_KEY)).isNull();
        }

        @Test
        void should_evict_existing_entry_from_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);

            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.evict(TEST_KEY)).isNotNull();
        }

        @Test
        void should_notify_listener_when_evicting_value_from_cache() {
            Cache<String, String> cache = new HazelcastCache<>(hazelcastInstance.getMap(CACHE_NAME), -1);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryEvicted(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.evict(TEST_KEY)).isNotNull();
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }
}
