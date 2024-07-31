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

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.ValueMapper;
import io.gravitee.node.plugin.cache.redis.configuration.HostAndPort;
import io.gravitee.node.plugin.cache.redis.configuration.RedisConfiguration;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class RedisCacheTest {

    private static RedisServer redisServer;
    private static Cache<String, String> redisCache;

    @BeforeAll
    public static void redisInitialization() throws IOException {
        redisServer = new RedisServer(6379);
        redisServer.start();

        final var redisConfiguration = new RedisConfiguration();
        redisConfiguration.setHostAndPort(HostAndPort.of("localhost", 6379));

        final var cm = new RedisCacheManager();
        cm.setVertx(Vertx.vertx());
        cm.setRedisConfiguration(redisConfiguration);
        redisCache =
            cm.getOrCreateCache(
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
    public void should_store_a_key() throws Exception {
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
    public void should_store_a_key_with_ttl() throws Exception {
        final var key = UUID.randomUUID().toString();
        TestObserver<String> test = redisCache.rxPut(key, "myvalue1", 5, TimeUnit.SECONDS).test();
        test.await();
        test.assertNoValues(); // when key is missing the put return nothing

        test = redisCache.rxGet(key).test();
        test.await();
        test.assertValue("myvalue1");

        Thread.sleep(6000);

        test = redisCache.rxGet(key).test();
        test.await();
        test.assertNoValues();
    }

    @Test
    public void should_store_all_map_entries() throws Exception {
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
    public void should_evict_a_key() throws Exception {
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
    public void should_be_able_to_clear_cache() throws Exception {
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
