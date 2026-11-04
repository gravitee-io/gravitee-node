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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Behaviour every {@link Cache} implementation must share, whatever its backend.
 * Each implementation runs it against a real server by extending this class.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AbstractCacheContractTest {

    protected static final String KEY = "key";

    /**
     * Create a new, empty cache. Each call must return a cache isolated from the ones created before.
     */
    protected abstract Cache<String, String> createCache(String name, CacheConfiguration configuration);

    protected Cache<String, String> newCache() {
        return newCache(CacheConfiguration.builder().build());
    }

    protected Cache<String, String> newCache(CacheConfiguration configuration) {
        return createCache("contract-" + UUID.randomUUID(), configuration);
    }

    @Nested
    class Put {

        @Test
        void should_return_nothing_when_key_is_absent() {
            Cache<String, String> cache = newCache();

            assertThat(cache.put(KEY, "a")).isNull();
            assertThat(cache.get(KEY)).isEqualTo("a");
        }

        @Test
        void should_replace_existing_value_and_return_previous_one() {
            Cache<String, String> cache = newCache();
            cache.put(KEY, "a");

            assertThat(cache.put(KEY, "b")).isEqualTo("a");
            assertThat(cache.get(KEY)).isEqualTo("b");
        }

        @Test
        void should_replace_existing_value_and_return_previous_one_reactively() {
            Cache<String, String> cache = newCache();
            cache.rxPut(KEY, "a").test().awaitDone(5, TimeUnit.SECONDS).assertNoErrors().assertNoValues();

            cache.rxPut(KEY, "b").test().awaitDone(5, TimeUnit.SECONDS).assertValue("a");
            cache.rxGet(KEY).test().awaitDone(5, TimeUnit.SECONDS).assertValue("b");
        }

        @Test
        void should_replace_existing_value_and_return_previous_one_with_ttl() {
            Cache<String, String> cache = newCache();
            cache.put(KEY, "a", 1, TimeUnit.MINUTES);

            assertThat(cache.put(KEY, "b", 1, TimeUnit.MINUTES)).isEqualTo("a");
            assertThat(cache.get(KEY)).isEqualTo("b");
        }

        @Test
        void should_store_all_entries() {
            Cache<String, String> cache = newCache();

            cache.putAll(Map.of("k1", "v1", "k2", "v2"));

            assertThat(cache.get("k1")).isEqualTo("v1");
            assertThat(cache.get("k2")).isEqualTo("v2");
        }
    }

    @Nested
    class ContainsKey {

        @Test
        void should_be_true_when_key_is_present() {
            Cache<String, String> cache = newCache();
            cache.put(KEY, "a");

            assertThat(cache.containsKey(KEY)).isTrue();
            cache.rxContainsKey(KEY).test().awaitDone(5, TimeUnit.SECONDS).assertValue(true);
        }

        @Test
        void should_be_false_when_key_is_absent() {
            Cache<String, String> cache = newCache();

            assertThat(cache.containsKey(KEY)).isFalse();
        }

        @Test
        void should_be_false_once_key_is_evicted() {
            Cache<String, String> cache = newCache();
            cache.put(KEY, "a");
            cache.evict(KEY);

            assertThat(cache.containsKey(KEY)).isFalse();
        }
    }

    @Nested
    class Evict {

        @Test
        void should_return_removed_value() {
            Cache<String, String> cache = newCache();
            cache.put(KEY, "a");

            assertThat(cache.evict(KEY)).isEqualTo("a");
            assertThat(cache.get(KEY)).isNull();
        }

        @Test
        void should_return_nothing_when_key_is_absent() {
            Cache<String, String> cache = newCache();

            cache.rxEvict(KEY).test().awaitDone(5, TimeUnit.SECONDS).assertNoErrors().assertNoValues();
        }

        @Test
        void should_remove_every_entry_on_clear() {
            Cache<String, String> cache = newCache();
            cache.put("k1", "v1");
            cache.put("k2", "v2");

            cache.clear();

            assertThat(cache.isEmpty()).isTrue();
            assertThat(cache.get("k1")).isNull();
        }
    }

    @Nested
    class TimeToLive {

        @Test
        void should_expire_entry_written_with_its_own_ttl() {
            Cache<String, String> cache = newCache();
            cache.put(KEY, "a", 500, TimeUnit.MILLISECONDS);

            assertThat(cache.get(KEY)).isEqualTo("a");
            await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(cache.get(KEY)).isNull());
        }

        @Test
        void should_never_expire_entry_written_with_a_ttl_of_zero() {
            Cache<String, String> cache = newCache();

            cache.put(KEY, "a", 0, TimeUnit.MILLISECONDS);

            assertThat(cache.get(KEY)).isEqualTo("a");
        }

        @Test
        void should_apply_configured_ttl_to_entry_written_without_one() {
            Cache<String, String> cache = newCache(CacheConfiguration.builder().timeToLiveInMs(1000).build());
            cache.put(KEY, "a");

            assertThat(cache.get(KEY)).isEqualTo("a");
            await()
                .atMost(4, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(cache.get(KEY)).isNull());
        }

        @Test
        void should_apply_configured_ttl_to_entry_written_reactively() {
            Cache<String, String> cache = newCache(CacheConfiguration.builder().timeToLiveInMs(1000).build());
            cache.rxPut(KEY, "a").test().awaitDone(5, TimeUnit.SECONDS).assertNoErrors();

            await()
                .atMost(4, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(cache.get(KEY)).isNull());
        }

        @Test
        void should_apply_configured_ttl_below_one_second() {
            Cache<String, String> cache = newCache(CacheConfiguration.builder().timeToLiveInMs(500).build());
            cache.put("sync", "a");
            cache.rxPut("reactive", "a").test().awaitDone(5, TimeUnit.SECONDS).assertNoErrors();

            assertThat(cache.get("sync")).isEqualTo("a");
            await()
                .atMost(4, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(cache.get("sync")).isNull();
                    assertThat(cache.get("reactive")).isNull();
                });
        }

        @Test
        void should_reject_a_ttl_bigger_than_the_configured_one() {
            Cache<String, String> cache = newCache(CacheConfiguration.builder().timeToLiveInMs(1000).build());

            assertThatThrownBy(() -> cache.put(KEY, "a", 1, TimeUnit.MINUTES)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Listeners {

        @Test
        void should_notify_added_updated_and_evicted_entries() {
            Cache<String, String> cache = newCache();
            RecordingListener listener = new RecordingListener();
            cache.addCacheListener(listener);

            cache.put(KEY, "a");
            await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(listener.events).containsExactly("added:key=a"));
            cache.put(KEY, "b");
            await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(listener.events).containsExactly("added:key=a", "updated:key=a->b"));
            cache.evict(KEY);
            await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(listener.events).containsExactly("added:key=a", "updated:key=a->b", "evicted:key=b"));
        }

        @Test
        void should_stop_notifying_a_removed_listener() {
            Cache<String, String> cache = newCache();
            RecordingListener listener = new RecordingListener();
            String id = cache.addCacheListener(listener);

            assertThat(cache.removeCacheListener(id)).isTrue();
            cache.put(KEY, "a");

            await().during(500, TimeUnit.MILLISECONDS).atMost(2, TimeUnit.SECONDS).until(listener.events::isEmpty);
        }

        @Test
        void should_return_false_when_removing_an_unknown_listener() {
            Cache<String, String> cache = newCache();

            assertThat(cache.removeCacheListener("unknown")).isFalse();
            assertThat(cache.removeCacheListener(UUID.randomUUID().toString())).isFalse();
        }
    }

    protected static class RecordingListener implements CacheListener<String, String> {

        public final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public void onEntryAdded(String key, String value) {
            events.add("added:" + key + "=" + value);
        }

        @Override
        public void onEntryUpdated(String key, String oldValue, String value) {
            events.add("updated:" + key + "=" + oldValue + "->" + value);
        }

        @Override
        public void onEntryEvicted(String key, String value) {
            events.add("evicted:" + key + "=" + value);
        }

        @Override
        public void onEntryExpired(String key, String value) {
            events.add("expired:" + key + "=" + value);
        }
    }
}
