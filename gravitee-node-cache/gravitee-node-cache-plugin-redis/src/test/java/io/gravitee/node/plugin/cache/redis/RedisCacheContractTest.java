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
import io.gravitee.node.plugin.cache.common.AbstractCacheContractTest;
import io.gravitee.node.vertx.client.redis.VertxRedisClientFactory;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import redis.embedded.RedisServer;

/**
 * @author GraviteeSource Team
 */
class RedisCacheContractTest extends AbstractCacheContractTest {

    private static final ValueMapper<String, String> IDENTITY = new ValueMapper<>() {
        @Override
        public String toCachedValue(String value) {
            return value;
        }

        @Override
        public String toValue(String cachedValue) {
            return cachedValue;
        }
    };

    private static RedisServer redisServer;
    private static Vertx vertx;
    private static RedisCacheManager cacheManager;

    @BeforeAll
    static void startRedis() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        redisServer = new RedisServer(port);
        redisServer.start();

        vertx = Vertx.vertx();
        cacheManager = new RedisCacheManager(
            RedisClientOptions.builder().host("localhost").port(port).build(),
            new VertxRedisClientFactory(vertx)
        );
    }

    @AfterAll
    static void stopRedis() throws Exception {
        cacheManager.stop();
        vertx.close().toCompletionStage().toCompletableFuture().get();
        redisServer.stop();
    }

    @Override
    protected Cache<String, String> createCache(String name, CacheConfiguration configuration) {
        return cacheManager.getOrCreateCache(name, configuration, IDENTITY);
    }
}
