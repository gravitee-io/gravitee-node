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
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
    private static final String TEST_KEY2 = "key2";
    private static final String TEST_VALUE2 = "value2";

    static HazelcastInstance hazelcastInstance;
    private HazelcastCacheManager hazelcastCacheManager;
    private String cacheName;

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
        hazelcastCacheManager = new HazelcastCacheManager(hazelcastInstance);
        cacheName = UUID.randomUUID().toString();
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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.get("no_key")).isNull();
        }

        @Test
        void should_return_null_when_entry_expired() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.size()).isZero();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_return_no_values_when_entry_expired() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS);

            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(cache.values()).isEmpty());
        }
    }

    @Nested
    class RxGetTest {

        @Test
        void should_return_null_when_getting_non_existing_key() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxGet("no_key").blockingGet()).isNull();
        }

        @Test
        void should_return_null_when_entry_expired() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS);

            await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.rxGet(TEST_KEY).blockingGet()).isNull();
                    assertThat(cache.rxValues().blockingIterable()).isEmpty();
                });
        }

        @Test
        void should_return_no_values_when_no_entry_in_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxSize().blockingGet()).isZero();
            assertThat(cache.rxValues().blockingIterable()).isEmpty();
        }

        @Test
        void should_return_no_values_when_entry_expired() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS);

            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(cache.rxValues().blockingIterable()).isEmpty());
        }
    }

    @Nested
    class PutTest {

        @Test
        void should_put_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE);

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_put_value_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
    class RxPutTest {

        @Test
        void should_put_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxPut(TEST_KEY, TEST_VALUE).blockingGet()).isNull();

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_put_value_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxPut(TEST_KEY, TEST_VALUE).blockingGet()).isNull();

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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxPut(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS).blockingGet()).isNull();
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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            assertThat(cache.rxPut(TEST_KEY, TEST_VALUE).blockingGet()).isNull();
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryUpdated(final String key, final String oldValue, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            assertThat(cache.rxPut(TEST_KEY, TEST_VALUE).blockingGet()).isNull();
            assertThat(cache.rxPut(TEST_KEY, TEST_VALUE_UPDATED).blockingGet()).isEqualTo(TEST_VALUE);

            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }

    @Nested
    class PutAllTest {

        @Test
        void should_put_all_values_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2));

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.get(TEST_KEY2)).isNotNull();
            assertThat(cache.values()).hasSize(2);
            assertThat(cache.values()).containsOnly(TEST_VALUE, TEST_VALUE2);
        }

        @Test
        void should_put_all_values_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2));
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.get(TEST_KEY2)).isNotNull();
            assertThat(cache.values()).hasSize(2);
            assertThat(cache.values()).containsOnly(TEST_VALUE, TEST_VALUE2);

            await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.get(TEST_KEY2)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_putting_new_values_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2));
            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryUpdated(final String key, final String oldValue, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2));
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE_UPDATED));
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }

    @Nested
    class RxPutAllTest {

        @Test
        void should_put_all_values_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.rxPutAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2)).blockingAwait();

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.get(TEST_KEY2)).isNotNull();
            assertThat(cache.values()).hasSize(2);
            assertThat(cache.values()).containsOnly(TEST_VALUE, TEST_VALUE2);
        }

        @Test
        void should_put_all_values_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.rxPutAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2)).blockingAwait();
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.get(TEST_KEY2)).isNotNull();
            assertThat(cache.values()).hasSize(2);
            assertThat(cache.values()).containsOnly(TEST_VALUE, TEST_VALUE2);

            await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.get(TEST_KEY2)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_putting_new_values_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.rxPutAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2)).blockingAwait();
            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryUpdated(final String key, final String oldValue, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.rxPutAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2)).blockingAwait();
            cache.rxPutAll(Map.of(TEST_KEY, TEST_VALUE_UPDATED)).blockingAwait();
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }

    @Nested
    class ComputeIfAbsentTest {

        @Test
        void should_do_nothing_if_key_already_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.computeIfAbsent(TEST_KEY, k -> "wrong value")).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_do_nothing_if_mapping_returns_null() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.computeIfAbsent(TEST_KEY, k -> null);
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_computeIfAbsent_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.computeIfAbsent(TEST_KEY, k -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_computeIfAbsent_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.computeIfAbsent(TEST_KEY, k -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
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
        void should_notify_listener_when_computeIfAbsent_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            assertThat(cache.computeIfAbsent(TEST_KEY, k -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }
    }

    @Nested
    class RxComputeIfAbsentTest {

        @Test
        void should_do_nothing_if_key_already_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.rxComputeIfAbsent(TEST_KEY, k -> "wrong value").blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_do_nothing_if_mapping_returns_null() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxComputeIfAbsent(TEST_KEY, k -> null).blockingGet()).isNull();
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_computeIfAbsent_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxComputeIfAbsent(TEST_KEY, k -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_computeIfAbsent_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxComputeIfAbsent(TEST_KEY, k -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
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
        void should_notify_listener_when_computeIfAbsent_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            assertThat(cache.rxComputeIfAbsent(TEST_KEY, k -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }
    }

    @Nested
    class ComputeIfPresentTest {

        @Test
        void should_do_nothing_if_key_not_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE)).isNull();
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_remove_if_mapping_returns_null() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.computeIfPresent(TEST_KEY, (k, v) -> null)).isNull();
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_computeIfPresent_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_computeIfPresent_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
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
        void should_notify_listener_when_computeIfPresent_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryUpdated(final String key, final String oldValue, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE_UPDATED)).isEqualTo(TEST_VALUE_UPDATED);
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }

    @Nested
    class RxComputeIfPresentTest {

        @Test
        void should_do_nothing_if_key_not_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxComputeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isNull();
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_remove_if_mapping_returns_null() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.rxComputeIfPresent(TEST_KEY, (k, v) -> null).blockingGet()).isNull();
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_computeIfPresent_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.rxComputeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_computeIfPresent_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.rxComputeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
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
        void should_notify_listener_when_computeIfPresent_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryUpdated(final String key, final String oldValue, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.rxComputeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.rxComputeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE_UPDATED).blockingGet()).isEqualTo(TEST_VALUE_UPDATED);

            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }

    @Nested
    class ComputeTest {

        @Test
        void should_compute_if_no_key_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.compute(TEST_KEY, (k, v) -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_remove_if_mapping_returns_null() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.compute(TEST_KEY, (k, v) -> null)).isNull();
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_compute_if_key_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.compute(TEST_KEY, (k, v) -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_compute_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.compute(TEST_KEY, (k, v) -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
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
        void should_notify_listener_when_compute_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            assertThat(cache.compute(TEST_KEY, (k, v) -> TEST_VALUE)).isEqualTo(TEST_VALUE);
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
            assertThat(cache.compute(TEST_KEY, (k, v) -> TEST_VALUE_UPDATED)).isEqualTo(TEST_VALUE_UPDATED);
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }
    }

    @Nested
    class RxComputeTest {

        @Test
        void should_compute_if_no_key_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxCompute(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_remove_if_mapping_returns_null() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.rxCompute(TEST_KEY, (k, v) -> null).blockingGet()).isNull();
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_compute_if_key_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            assertThat(cache.rxCompute(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_compute_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(1000).build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            assertThat(cache.rxCompute(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);
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
        void should_notify_listener_when_compute_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            assertThat(cache.rxCompute(TEST_KEY, (k, v) -> TEST_VALUE).blockingGet()).isEqualTo(TEST_VALUE);
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
            assertThat(cache.rxCompute(TEST_KEY, (k, v) -> TEST_VALUE_UPDATED).blockingGet()).isEqualTo(TEST_VALUE_UPDATED);

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
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);

            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.evict(TEST_KEY)).isNull();
        }

        @Test
        void should_evict_existing_entry_from_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);

            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.evict(TEST_KEY)).isNotNull();
            assertThat(cache.get(TEST_KEY)).isNull();
        }

        @Test
        void should_notify_listener_when_evicting_value_from_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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
            assertThat(cache.get(TEST_KEY)).isNull();
            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }
    }

    @Nested
    class rxEvictTest {

        @Test
        void should_ignore_evicting_not_existing_entry_from_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);

            cache.rxEvict(TEST_KEY).test().awaitDone(1, TimeUnit.SECONDS).assertNoValues().assertComplete();
        }

        @Test
        void should_evict_existing_entry_from_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);

            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isEqualTo(TEST_VALUE);

            cache.rxEvict(TEST_KEY).test().awaitDone(1, TimeUnit.SECONDS).assertValue(TEST_VALUE);
        }

        @Test
        void should_notify_listener_when_evicting_value_from_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = hazelcastCacheManager.getOrCreateCache(CACHE_NAME, configuration);
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

            cache.rxEvict(TEST_KEY).test().awaitDone(1, TimeUnit.SECONDS).assertValue(TEST_VALUE);

            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }
    }
}
