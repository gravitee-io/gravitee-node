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
package io.gravitee.node.plugin.cache.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InMemoryCacheTest {

    private static final String CACHE_NAME = "test-cache";
    private static final String TEST_KEY = "key1";
    private static final String TEST_VALUE = "value1";
    private static final String TEST_VALUE_UPDATED = "value1_updated";
    private static final String TEST_KEY2 = "key2";
    private static final String TEST_VALUE2 = "value2";

    @Nested
    class GetTest {

        @Test
        void should_return_null_when_getting_non_existing_key() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            assertThat(cache.get("no_key")).isNull();
        }

        @Test
        void should_return_null_when_entry_expired() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.MILLISECONDS);

            await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_return_no_values_when_no_entry_in_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            assertThat(cache.size()).isZero();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_return_no_values_when_entry_expired() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.MILLISECONDS);

            await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(cache.values()).isEmpty());
        }
    }

    @Nested
    class PutTest {

        @Test
        void should_put_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE);

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_put_value_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(200).build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);

            await()
                .atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_put_value_to_cache_with_custom_ttl_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE, 200, TimeUnit.MILLISECONDS);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);

            await()
                .atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_putting_new_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
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
            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
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
    class PutAllTest {

        @Test
        void should_put_all_values_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2));

            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.get(TEST_KEY2)).isNotNull();
            assertThat(cache.values()).hasSize(2);
            assertThat(cache.values()).containsOnly(TEST_VALUE, TEST_VALUE2);
        }

        @Test
        void should_put_all_values_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(200).build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.putAll(Map.of(TEST_KEY, TEST_VALUE, TEST_KEY2, TEST_VALUE2));
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.get(TEST_KEY2)).isNotNull();
            assertThat(cache.values()).hasSize(2);
            assertThat(cache.values()).containsOnly(TEST_VALUE, TEST_VALUE2);

            await()
                .atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.get(TEST_KEY2)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_putting_new_values_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
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
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
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
    class ComputeIfAbsentTest {

        @Test
        void should_do_nothing_if_key_already_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, TEST_VALUE);
            cache.computeIfAbsent(TEST_KEY, k -> "wrong value");
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_computeIfAbsent_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.computeIfAbsent(TEST_KEY, k -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_computeIfAbsent_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(200).build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.computeIfAbsent(TEST_KEY, k -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);

            await()
                .atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_computeIfAbsent_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.computeIfAbsent(TEST_KEY, k -> TEST_VALUE);
            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(listenerCalled).isTrue());
        }
    }

    @Nested
    class ComputeIfPresentTest {

        @Test
        void should_do_nothing_if_key_not_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNull();
            assertThat(cache.values()).isEmpty();
        }

        @Test
        void should_computeIfPresent_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_computeIfPresent_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(200).build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);

            await()
                .atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_computeIfPresent_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
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
            cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE);
            cache.computeIfPresent(TEST_KEY, (k, v) -> TEST_VALUE_UPDATED);
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
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.compute(TEST_KEY, (k, v) -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_compute_if_key_present_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.put(TEST_KEY, "wrong_value");
            cache.compute(TEST_KEY, (k, v) -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);
        }

        @Test
        void should_compute_to_cache_and_expired_after_ttl() {
            CacheConfiguration configuration = CacheConfiguration.builder().timeToLiveInMs(200).build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            cache.compute(TEST_KEY, (k, v) -> TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.values()).hasSize(1);
            assertThat(cache.values()).containsOnly(TEST_VALUE);

            await()
                .atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get(TEST_KEY)).isNull();
                    assertThat(cache.values()).isEmpty();
                });
        }

        @Test
        void should_notify_listener_when_compute_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryAdded(final String key, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.compute(TEST_KEY, (k, v) -> TEST_VALUE);
            await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(listenerCalled).isTrue();
                });
        }

        @Test
        void should_notify_listener_when_putting_updated_value_to_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
            AtomicBoolean listenerCalled = new AtomicBoolean();
            cache.addCacheListener(
                new CacheListener<>() {
                    @Override
                    public void onEntryUpdated(final String key, final String oldValue, final String value) {
                        listenerCalled.set(true);
                    }
                }
            );
            cache.compute(TEST_KEY, (k, v) -> TEST_VALUE);
            cache.compute(TEST_KEY, (k, v) -> TEST_VALUE_UPDATED);
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
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);

            assertThat(cache.evict(TEST_KEY)).isNull();
        }

        @Test
        void should_evict_existing_entry_from_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);

            cache.put(TEST_KEY, TEST_VALUE);
            assertThat(cache.get(TEST_KEY)).isNotNull();
            assertThat(cache.evict(TEST_KEY)).isNotNull();
        }

        @Test
        void should_notify_listener_when_evicting_value_from_cache() {
            CacheConfiguration configuration = CacheConfiguration.builder().build();
            Cache<String, String> cache = new InMemoryCache<>(CACHE_NAME, configuration);
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
