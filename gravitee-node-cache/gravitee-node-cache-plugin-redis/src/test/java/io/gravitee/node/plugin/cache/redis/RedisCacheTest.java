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

package io.gravitee.node.plugin.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheException;
import io.gravitee.node.api.cache.ValueMapper;
import io.gravitee.node.vertx.client.redis.VertxRedisClientFactory;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.Vertx;
import io.vertx.redis.client.RedisAPI;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import redis.embedded.RedisServer;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RedisCacheTest {

    private static RedisServer redisServer;
    private static Cache<String, String> redisCache;
    private static RedisAPI redisAPI;
    private static RedisCacheManager cacheManager;

    @BeforeAll
    public static void redisInitialization() throws IOException {
        // A free port, so the tests never reach a Redis already running on this machine
        final int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        redisServer = new RedisServer(port);
        redisServer.start();

        final var options = RedisClientOptions.builder().host("localhost").port(port).build();
        final var vertx = Vertx.vertx();
        final var redisClientFactory = new VertxRedisClientFactory(vertx);
        redisAPI = RedisAPI.api(redisClientFactory.createClient(options));
        cacheManager = new RedisCacheManager(options, redisClientFactory);
        redisCache = cacheManager.getOrCreateCache(
            "test",
            CacheConfiguration.builder().build(),
            new ValueMapper<String, String>() {
                @Override
                public String toCachedValue(String value) {
                    return value;
                }

                @Override
                public String toValue(String cachedValue) {
                    return cachedValue;
                }
            }
        );
    }

    @Test
    void should_store_a_key() throws Exception {
        final var key = UUID.randomUUID().toString();
        TestObserver<String> test = redisCache.rxPut(key, "myvalue1").test();
        test.await();
        test.assertNoValues(); // when key is missing the put return nothing

        test = redisCache.rxGet(key).test();
        test.await();
        test.assertValue("myvalue1");

        test = redisCache.rxPut(key, "myvalue2").test();
        test.await();
        test.assertValue("myvalue1"); // when key is present the put return previous value
    }

    @Test
    void should_report_redis_error_when_value_cannot_be_read() throws Exception {
        final var key = UUID.randomUUID().toString();
        // A list under the entry key makes GET fail with WRONGTYPE
        final var entryKey = "test" + RedisCache.SEPARATOR + key;
        redisAPI.lpush(List.of(entryKey, "item")).toCompletionStage().toCompletableFuture().get();

        try {
            redisCache
                .rxGet(key)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(e -> e instanceof CacheException && "Value cannot be read from cache".equals(e.getMessage()))
                .assertError(e -> e.getCause() != null && e.getCause().getMessage().startsWith("WRONGTYPE"));
        } finally {
            redisAPI.del(List.of(entryKey)).toCompletionStage().toCompletableFuture().get();
        }
    }

    @Test
    void should_count_a_replaced_value_it_cannot_read_as_absent() throws Exception {
        // A value written by another version, in a format this mapper cannot read
        Cache<String, String> cache = cacheManager.getOrCreateCache(
            "format-change",
            CacheConfiguration.builder().build(),
            new ValueMapper<String, String>() {
                @Override
                public String toCachedValue(String value) {
                    return "v2:" + value;
                }

                @Override
                public String toValue(String cachedValue) {
                    if (!cachedValue.startsWith("v2:")) {
                        throw new IllegalStateException("unknown format");
                    }
                    return cachedValue.substring(3);
                }
            }
        );
        final var key = UUID.randomUUID().toString();
        final var entryKey = "format-change" + RedisCache.SEPARATOR + key;
        List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        cache.addCacheListener(
            new io.gravitee.node.api.cache.CacheListener<>() {
                @Override
                public void onEntryAdded(String k, String v) {
                    events.add("added:" + v);
                }
            }
        );

        redisAPI.set(List.of(entryKey, "v1:old")).toCompletionStage().toCompletableFuture().get();
        cache.rxPut(key, "new").test().awaitDone(5, TimeUnit.SECONDS).assertNoErrors().assertNoValues();
        assertThat(cache.get(key)).isEqualTo("new");
        assertThat(events).containsExactly("added:new");

        redisAPI.set(List.of(entryKey, "v1:old")).toCompletionStage().toCompletableFuture().get();
        cache.rxEvict(key).test().awaitDone(5, TimeUnit.SECONDS).assertNoErrors().assertNoValues();
        assertThat(cache.containsKey(key)).isFalse();
    }

    @Test
    void should_store_a_key_with_ttl() throws Exception {
        final var key = UUID.randomUUID().toString();
        TestObserver<String> test = redisCache.rxPut(key, "myvalue1", 3, TimeUnit.SECONDS).test();
        test.await();
        test.assertNoValues(); // when key is missing the put return nothing

        test = redisCache.rxGet(key).test();
        test.await();
        test.assertValue("myvalue1");

        Awaitility.await()
            .atLeast(2, TimeUnit.SECONDS)
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                var get = redisCache.rxGet(key).test();
                get.await();
                get.assertNoValues();
            });
    }

    @Test
    void should_store_all_map_entries() throws Exception {
        final var entries = Map.of("key1", UUID.randomUUID().toString(), "key2", UUID.randomUUID().toString());

        var testGet = redisCache.rxGet("key1").test();
        testGet.await();
        testGet.assertNoValues();

        var testPutAll = redisCache.rxPutAll(entries).test();
        testPutAll.await();
        testPutAll.assertNoErrors();

        testGet = redisCache.rxGet("key1").test();
        testGet.await();
        testGet.assertValue(entries.get("key1"));
    }

    @Test
    void should_evict_a_key() throws Exception {
        final var key = UUID.randomUUID().toString();
        TestObserver<String> test = redisCache.rxPut(key, "myvalue1").test();
        test.await();
        test.assertNoValues(); // when key is missing the put return nothing

        test = redisCache.rxGet(key).test();
        test.await();
        test.assertValue("myvalue1");

        test = redisCache.rxEvict(key).test();
        test.await();
        test.assertValue("myvalue1"); // evict provide removed value

        test = redisCache.rxGet(key).test();
        test.await();
        test.assertNoValues();
    }

    @Test
    void should_be_able_to_clear_cache() throws Exception {
        // clear cache to remove all data from previous tests
        var testClear = redisCache.rxClear().test();
        testClear.await();
        testClear.assertNoValues();
        testClear.assertNoErrors();

        // cache should be empty
        var testEmpty = redisCache.rxIsEmpty().test();
        testEmpty.await();
        testEmpty.assertValue(true);

        // insert entry
        final var key = UUID.randomUUID().toString();
        TestObserver<String> test = redisCache.rxPut(key, "myvalue1").test();
        test.await();
        test.assertNoValues(); // when key is missing the put return nothing

        // Now test empty and clear operations
        testEmpty = redisCache.rxIsEmpty().test();
        testEmpty.await();
        testEmpty.assertValue(false);

        testClear = redisCache.rxClear().test();
        testClear.await();
        testClear.assertNoValues();
        testClear.assertNoErrors();

        testEmpty = redisCache.rxIsEmpty().test();
        testEmpty.await();
        testEmpty.assertValue(true);
    }

    @AfterAll
    public static void shutdownRedisServer() {
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (IOException e) {
                log.warn("Unable to stop Redis Server: {}", e.getMessage());
            }
        }
    }
}
