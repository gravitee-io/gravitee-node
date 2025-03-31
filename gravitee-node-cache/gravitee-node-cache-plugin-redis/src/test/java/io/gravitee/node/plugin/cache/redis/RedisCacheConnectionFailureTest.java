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
import io.gravitee.node.api.cache.CacheException;
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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RedisCacheConnectionFailureTest {

    private static Cache<String, String> redisCache;

    @BeforeAll
    public static void redisInitialization() throws IOException {
        final var redisConfiguration = new RedisConfiguration();
        redisConfiguration.setHostAndPort(HostAndPort.of("test", 6379));

        final var cm = new RedisCacheManager(redisConfiguration, Vertx.vertx());
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
    void should_store_a_key() throws Exception {
        final var key = UUID.randomUUID().toString();
        TestObserver<String> test = redisCache.rxPut(key, "myvalue1").test();
        test.await();
        test.assertError(CacheException.class);
    }

    @Test
    void should_store_a_key_with_ttl() throws Exception {
        final var key = UUID.randomUUID().toString();
        TestObserver<String> test = redisCache.rxPut(key, "myvalue1", 3, TimeUnit.SECONDS).test();
        test.await();
        test.assertError(CacheException.class);
    }

    @Test
    void should_store_all_map_entries() throws Exception {
        final var entries = Map.of("key1", UUID.randomUUID().toString(), "key2", UUID.randomUUID().toString());

        var testPutAll = redisCache.rxPutAll(entries).test();
        testPutAll.await();
        testPutAll.assertError(CacheException.class);
    }

    @Test
    void should_evict_a_key() throws Exception {
        final var key = UUID.randomUUID().toString();
        final var test = redisCache.rxEvict(key).test();
        test.await();
        test.assertError(CacheException.class);
    }

    @Test
    void should_be_able_to_clear_cache() throws Exception {
        var testClear = redisCache.rxClear().test();
        testClear.await();
        testClear.assertError(CacheException.class);
    }
}
