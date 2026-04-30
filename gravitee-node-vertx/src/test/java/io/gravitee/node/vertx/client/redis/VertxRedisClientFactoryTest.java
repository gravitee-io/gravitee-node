/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.vertx.client.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.plugin.configurations.redis.HostAndPort;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.redis.RedisSentinelOptions;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VertxRedisClientFactoryTest {

    private final io.vertx.core.Vertx vertx = io.vertx.core.Vertx.vertx();
    private final VertxRedisClientFactory factory = new VertxRedisClientFactory(vertx);

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    @Nested
    class BuildRedisOptions {

        @Test
        void shouldBuildStandaloneRedisOptions() {
            var options = RedisClientOptions.builder().host("redis.local").port(6380).password("secret").build();

            RedisOptions result = factory.buildRedisOptions(options);

            assertThat(result.getType()).isEqualTo(RedisClientType.STANDALONE);
            assertThat(result.getEndpoint()).contains("redis.local:6380");
            assertThat(result.getPoolName()).startsWith("gravitee-redis-");
        }

        @Test
        void shouldBuildSentinelRedisOptions() {
            var sentinel = RedisSentinelOptions
                .builder()
                .masterId("mymaster")
                .password("sentinelpw")
                .nodes(List.of(HostAndPort.builder().host("s1").port(26379).build(), HostAndPort.builder().host("s2").port(26380).build()))
                .build();

            var options = RedisClientOptions.builder().host("redis.local").port(6379).sentinel(sentinel).build();

            RedisOptions result = factory.buildRedisOptions(options);

            assertThat(result.getType()).isEqualTo(RedisClientType.SENTINEL);
            assertThat(result.getMasterName()).isEqualTo("mymaster");
        }

        @Test
        void shouldBuildSslRedisOptions() {
            var options = RedisClientOptions.builder().host("redis.local").port(6379).useSsl(true).build();

            RedisOptions result = factory.buildRedisOptions(options);

            assertThat(result.getNetClientOptions().isSsl()).isTrue();
            assertThat(result.getNetClientOptions().isTrustAll()).isTrue();
            assertThat(result.getEndpoint()).startsWith("rediss://");
        }

        @Test
        void shouldApplySslWithTrustStore() {
            var trustStore = JKSTrustStore.builder().path("/certs/truststore.jks").password("changeit").build();
            var ssl = SslOptions.builder().trustAll(false).trustStore(trustStore).build();
            var options = RedisClientOptions.builder().host("redis.local").port(6379).useSsl(true).ssl(ssl).build();

            RedisOptions result = factory.buildRedisOptions(options);

            assertThat(result.getNetClientOptions().isSsl()).isTrue();
            assertThat(result.getNetClientOptions().isTrustAll()).isFalse();
            assertThat(result.getNetClientOptions().getTrustOptions()).isNotNull();
        }

        @Test
        void shouldApplyPoolAndTimeoutSettings() {
            var options = RedisClientOptions
                .builder()
                .host("redis.local")
                .port(6379)
                .maxPoolSize(10)
                .maxPoolWaiting(200)
                .poolCleanerInterval(60000)
                .poolRecycleTimeout(300000)
                .maxWaitingHandlers(500)
                .connectTimeout(10000)
                .build();

            RedisOptions result = factory.buildRedisOptions(options);

            assertThat(result.getMaxPoolSize()).isEqualTo(10);
            assertThat(result.getMaxPoolWaiting()).isEqualTo(200);
            assertThat(result.getPoolCleanerInterval()).isEqualTo(60000);
            assertThat(result.getPoolRecycleTimeout()).isEqualTo(300000);
            assertThat(result.getMaxWaitingHandlers()).isEqualTo(500);
            assertThat(result.getNetClientOptions().getConnectTimeout()).isEqualTo(10000);
        }
    }

    @Nested
    class Dedup {

        @Test
        void shouldShareClientForSameConnectionTuple() {
            var opts1 = RedisClientOptions.builder().host("redis.local").port(6379).build();
            var opts2 = RedisClientOptions.builder().host("redis.local").port(6379).build();

            Redis c1 = factory.acquire(opts1);
            Redis c2 = factory.acquire(opts2);

            assertThat(c1).isSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(1);
        }

        @Test
        void shouldCreateSeparateClientsForDifferentHost() {
            Redis c1 = factory.acquire(RedisClientOptions.builder().host("redis1.local").port(6379).build());
            Redis c2 = factory.acquire(RedisClientOptions.builder().host("redis2.local").port(6379).build());

            assertThat(c1).isNotSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(2);
        }

        @Test
        void shouldCreateSeparateClientsForDifferentPort() {
            Redis c1 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).build());
            Redis c2 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6380).build());

            assertThat(c1).isNotSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(2);
        }

        @Test
        void shouldCreateSeparateClientsForDifferentSslFlag() {
            Redis c1 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).useSsl(false).build());
            Redis c2 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).useSsl(true).build());

            assertThat(c1).isNotSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(2);
        }

        @Test
        void shouldCreateSeparateClientsForDifferentUsername() {
            Redis c1 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).username("alice").build());
            Redis c2 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).username("bob").build());

            assertThat(c1).isNotSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(2);
        }

        @Test
        void shouldCreateSeparateClientsForDifferentSentinelNodes() {
            var sentinelA = RedisSentinelOptions
                .builder()
                .masterId("mymaster")
                .nodes(List.of(HostAndPort.builder().host("s1").port(26379).build()))
                .build();
            var sentinelB = RedisSentinelOptions
                .builder()
                .masterId("mymaster")
                .nodes(List.of(HostAndPort.builder().host("s2").port(26379).build()))
                .build();

            Redis c1 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).sentinel(sentinelA).build());
            Redis c2 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).sentinel(sentinelB).build());

            assertThat(c1).isNotSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(2);
        }

        @Test
        void shouldShareClientWhenSentinelNodesDeclaredInDifferentOrder() {
            var nodeA = HostAndPort.builder().host("s1").port(26379).build();
            var nodeB = HostAndPort.builder().host("s2").port(26380).build();

            var sentinelAB = RedisSentinelOptions.builder().masterId("mymaster").nodes(List.of(nodeA, nodeB)).build();
            var sentinelBA = RedisSentinelOptions.builder().masterId("mymaster").nodes(List.of(nodeB, nodeA)).build();

            Redis c1 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).sentinel(sentinelAB).build());
            Redis c2 = factory.acquire(RedisClientOptions.builder().host("redis.local").port(6379).sentinel(sentinelBA).build());

            assertThat(c1).isSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(1);
        }

        @Test
        void shouldCreateSeparateClientsForDifferentPassword() {
            // Password is part of the dedup key so tenants with different credentials
            // to the same Redis endpoint get separate clients (security boundary).
            var opts1 = RedisClientOptions.builder().host("redis.local").port(6379).password("p1").build();
            var opts2 = RedisClientOptions.builder().host("redis.local").port(6379).password("p2").build();

            Redis c1 = factory.acquire(opts1);
            Redis c2 = factory.acquire(opts2);

            assertThat(c1).isNotSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(2);
        }

        @Test
        void shouldShareClientForSamePassword() {
            var opts1 = RedisClientOptions.builder().host("redis.local").port(6379).password("same").build();
            var opts2 = RedisClientOptions.builder().host("redis.local").port(6379).password("same").build();

            Redis c1 = factory.acquire(opts1);
            Redis c2 = factory.acquire(opts2);

            assertThat(c1).isSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(1);
        }

        @Test
        void shouldShareClientDespiteDifferentPoolConfig() {
            // Pool sizing is not part of the dedup key. First-acquire wins.
            var opts1 = RedisClientOptions.builder().host("redis.local").port(6379).maxPoolSize(6).build();
            var opts2 = RedisClientOptions.builder().host("redis.local").port(6379).maxPoolSize(50).build();

            Redis c1 = factory.acquire(opts1);
            Redis c2 = factory.acquire(opts2);

            assertThat(c1).isSameAs(c2);
            assertThat(factory.sharedClientCount()).isEqualTo(1);
        }
    }

    @Nested
    class RefCounting {

        @Test
        void shouldCloseClientOnlyWhenRefCountReachesZero() {
            var opts = RedisClientOptions.builder().host("redis.local").port(6379).build();

            factory.acquire(opts);
            factory.acquire(opts);
            assertThat(factory.sharedClientCount()).isEqualTo(1);

            factory.release(opts);
            assertThat(factory.sharedClientCount()).isEqualTo(1);

            factory.release(opts);
            assertThat(factory.sharedClientCount()).isZero();
        }

        @Test
        void shouldCreateNewClientAfterFullRelease() {
            var opts = RedisClientOptions.builder().host("redis.local").port(6379).build();

            Redis first = factory.acquire(opts);
            factory.release(opts);
            assertThat(factory.sharedClientCount()).isZero();

            Redis second = factory.acquire(opts);

            assertThat(second).isNotSameAs(first);
            assertThat(factory.sharedClientCount()).isEqualTo(1);
            factory.release(opts);
        }

        @Test
        void shouldBeNoOpWhenReleasingUnknownClient() {
            var opts = RedisClientOptions.builder().host("unknown.local").port(6379).build();

            factory.release(opts);

            assertThat(factory.sharedClientCount()).isZero();
        }

        @Test
        void shouldRejectNullOptionsOnAcquire() {
            assertThatThrownBy(() -> factory.acquire(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
        }

        @Test
        void shouldIgnoreNullOptionsOnRelease() {
            // Defensive: symmetric with try/finally patterns where acquire may have thrown.
            factory.release(null);
            assertThat(factory.sharedClientCount()).isZero();
        }
    }

    @Nested
    class DedicatedClient {

        @Test
        void shouldCreateFreshClientWithoutTracking() {
            var opts = RedisClientOptions.builder().host("redis.local").port(6379).build();

            Redis c1 = factory.createClient(opts);
            Redis c2 = factory.createClient(opts);

            assertThat(c1).isNotSameAs(c2);
            assertThat(factory.sharedClientCount()).isZero();

            c1.close();
            c2.close();
        }

        @Test
        void shouldNotAffectOrBeAffectedByAcquireOnSameOptions() {
            // Dedicated and shared paths are independent: createClient() never
            // looks at or mutates the shared registry, and vice versa.
            var opts = RedisClientOptions.builder().host("redis.local").port(6379).build();

            Redis shared = factory.acquire(opts);
            Redis dedicated = factory.createClient(opts);

            assertThat(shared).isNotSameAs(dedicated);
            assertThat(factory.sharedClientCount()).isEqualTo(1);

            dedicated.close();
            factory.release(opts);
            assertThat(factory.sharedClientCount()).isZero();
        }

        @Test
        void shouldRejectNullOptions() {
            assertThatThrownBy(() -> factory.createClient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
        }
    }

    @Nested
    class Concurrency {

        @Test
        void shouldCreateSingleClientWhenAcquiredConcurrently() throws Exception {
            var opts = RedisClientOptions.builder().host("redis.local").port(6379).build();
            int threads = 16;

            CountDownLatch gate = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            Set<Redis> seen = java.util.Collections.synchronizedSet(new HashSet<>());

            try {
                for (int i = 0; i < threads; i++) {
                    pool.submit(() -> {
                        try {
                            gate.await();
                            seen.add(factory.acquire(opts));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
                }
                gate.countDown();
                assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

                assertThat(seen).hasSize(1);
                assertThat(factory.sharedClientCount()).isEqualTo(1);
            } finally {
                pool.shutdownNow();
            }

            // drain refs
            for (int i = 0; i < threads; i++) {
                factory.release(opts);
            }
            assertThat(factory.sharedClientCount()).isZero();
        }
    }

    @Nested
    class HostnameAlgorithmResolution {

        @Test
        void shouldUseExplicitAlgorithmWhenProvided() {
            var ssl = io.gravitee.node.vertx.client.ssl.SslOptions
                .builder()
                .hostnameVerifier(true)
                .hostnameVerificationAlgorithm("LDAPS")
                .build();
            assertThat(VertxRedisClientFactory.resolveHostnameVerificationAlgorithm(ssl)).isEqualTo("LDAPS");
        }

        @Test
        void shouldFallBackToHttpsWhenAlgorithmNoneAndVerifierTrue() {
            var ssl = io.gravitee.node.vertx.client.ssl.SslOptions
                .builder()
                .hostnameVerifier(true)
                .hostnameVerificationAlgorithm("NONE")
                .build();
            assertThat(VertxRedisClientFactory.resolveHostnameVerificationAlgorithm(ssl)).isEqualTo("HTTPS");
        }

        @Test
        void shouldFallBackToEmptyWhenAlgorithmNoneAndVerifierFalse() {
            var ssl = io.gravitee.node.vertx.client.ssl.SslOptions
                .builder()
                .hostnameVerifier(false)
                .hostnameVerificationAlgorithm("NONE")
                .build();
            assertThat(VertxRedisClientFactory.resolveHostnameVerificationAlgorithm(ssl)).isEmpty();
        }

        @Test
        void shouldFallBackToHttpsWhenAlgorithmNullAndVerifierTrue() {
            var ssl = io.gravitee.node.vertx.client.ssl.SslOptions.builder().hostnameVerifier(true).build();
            // hostnameVerificationAlgorithm builder-default is "NONE" — explicit-null only via setter.
            ssl.setHostnameVerificationAlgorithm(null);
            assertThat(VertxRedisClientFactory.resolveHostnameVerificationAlgorithm(ssl)).isEqualTo("HTTPS");
        }

        @Test
        void explicitAlgorithmTakesPrecedenceOverVerifierFalse() {
            // Verifier=false would normally disable verification, but an explicit
            // algorithm string opts the operator into algorithm-driven verification.
            var ssl = io.gravitee.node.vertx.client.ssl.SslOptions
                .builder()
                .hostnameVerifier(false)
                .hostnameVerificationAlgorithm("HTTPS")
                .build();
            assertThat(VertxRedisClientFactory.resolveHostnameVerificationAlgorithm(ssl)).isEqualTo("HTTPS");
        }
    }
}
