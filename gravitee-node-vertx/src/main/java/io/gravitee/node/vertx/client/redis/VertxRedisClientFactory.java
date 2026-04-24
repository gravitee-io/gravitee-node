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

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.plugin.configurations.redis.HostAndPort;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.redis.RedisSentinelOptions;
import io.gravitee.plugin.mappers.RedisClientOptionsMapper;
import io.gravitee.plugin.mappers.SslOptionsMapper;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Factory for Vert.x Redis clients. Two usage modes, both going through the same
 * {@link RedisClientOptions} + {@code RedisClientOptionsMapper} + node SSL plumbing,
 * so consumers don't duplicate the SSL / sentinel / base64 / pool-mapping code.
 *
 * <p><b>Shared client (opt-in, ref-counted):</b> {@link #acquire(RedisClientOptions)} /
 * {@link #release(RedisClientOptions)}. The factory dedups by connection-identifying
 * fields so multiple consumers pointing at the same endpoint share one underlying
 * Vert.x client and pool. When the last consumer releases, the client is closed.
 * Use this for per-API consumers that would otherwise fan out (e.g. the cache-redis
 * resource: 5000 APIs → 1 shared connection).
 *
 * <p><b>Dedicated client (default):</b> {@link #createClient(RedisClientOptions)}
 * returns a fresh Vert.x Redis client. No dedup, no ref counting, not tracked by the
 * factory — the caller owns the lifecycle and must close it. Use this for
 * gateway-singleton consumers (rate-limit, distributed-sync, node-cache-plugin-redis)
 * that already have one connection per gateway by Spring-bean lifecycle and don't
 * need or want cross-consumer sharing.
 *
 * <p><b>Dedup key:</b>
 * <ul>
 *   <li>{@code host}, {@code port}</li>
 *   <li>{@code useSsl}</li>
 *   <li>{@code username}</li>
 *   <li>{@code password} (never logged)</li>
 *   <li>sentinel {@code masterId} and sorted {@code nodes}</li>
 * </ul>
 *
 * <p><b>Invariant:</b> callers using the same connection tuple are assumed to use the
 * same SSL material and pool config. First acquire wins for those non-keyed fields.
 * The key can be extended later in a non-breaking way if that assumption changes for
 * a future consumer.
 *
 * <p><b>Security note:</b> the dedup key is kept internal to this class and never logged.
 * Log statements include only non-sensitive identifying fields (host, port, useSsl).
 *
 * <p>Instantiate via a {@code @Bean} method in a {@code @Configuration} class; {@code Vertx}
 * is supplied via constructor.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class VertxRedisClientFactory {

    private final ConcurrentHashMap<String, SharedRedisClient> sharedClients = new ConcurrentHashMap<>();

    private final Vertx vertx;

    /**
     * Acquire a shared Redis client for the given options. If a client with the same
     * dedup key already exists, its reference count is incremented and the existing
     * client is returned. Otherwise a new client is created.
     */
    public Redis acquire(RedisClientOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("RedisClientOptions must not be null");
        }
        String key = dedupKey(options);
        SharedRedisClient shared = sharedClients.compute(
            key,
            (k, existing) -> {
                if (existing != null) {
                    existing.refCount.incrementAndGet();
                    log.debug("Reusing Redis client for {}, refs={}", summary(options), existing.refCount.get());
                    return existing;
                }
                Redis client = Redis.createClient(vertx, buildRedisOptions(options));
                log.info("Created Redis client for {}", summary(options));
                return new SharedRedisClient(client, new AtomicInteger(1));
            }
        );
        return shared.client;
    }

    /**
     * Release a shared Redis client for the given options. When the reference count
     * reaches zero, the client is closed and removed from the registry. No-op if no
     * matching client exists.
     */
    public void release(RedisClientOptions options) {
        if (options == null) {
            return;
        }
        String key = dedupKey(options);
        sharedClients.compute(
            key,
            (k, existing) -> {
                if (existing == null) {
                    log.warn("Attempted to release unknown Redis client for {}", summary(options));
                    return null;
                }
                int remaining = existing.refCount.decrementAndGet();
                if (remaining <= 0) {
                    log.info("Closing Redis client for {}, no more references", summary(options));
                    try {
                        existing.client.close();
                    } catch (Exception e) {
                        log.warn("Failed to close Redis client: {}", e.getMessage());
                    }
                    return null;
                }
                log.debug("Released Redis client for {}, refs={}", summary(options), remaining);
                return existing;
            }
        );
    }

    /**
     * Build a fresh, dedicated Vert.x Redis client from the given options. Not tracked
     * by the factory. Caller owns the lifecycle and must close it.
     *
     * <p>Use this when the consumer is already a gateway singleton and doesn't need
     * cross-consumer sharing (rate-limit, distributed-sync, node-cache-plugin-redis).
     * The value in routing through the factory is the shared option-building plumbing
     * (SSL via node SSL classes, sentinel config, base64 decode, pool mapping), not
     * the shared registry.
     */
    public Redis createClient(RedisClientOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("RedisClientOptions must not be null");
        }
        log.info("Created dedicated Redis client for {}", summary(options));
        return Redis.createClient(vertx, buildRedisOptions(options));
    }

    @VisibleForTesting
    RedisOptions buildRedisOptions(RedisClientOptions options) {
        RedisOptions redisOptions = RedisClientOptionsMapper.INSTANCE.map(options);
        redisOptions.setPoolName("gravitee-redis-" + UUID.randomUUID());

        if (options.isUseSsl()) {
            configureSsl(redisOptions.getNetClientOptions(), options);
        }

        return redisOptions;
    }

    /**
     * Apply SSL config via the node SSL classes. Same pattern as
     * {@code VertxHttpClientFactory}. Base64 decoding of JKS/PKCS12 binary content
     * happens inside the node SSL classes' {@code trustOptions()} / {@code keyCertOptions()}.
     */
    private static void configureSsl(NetClientOptions netClientOptions, RedisClientOptions options) {
        netClientOptions.setSsl(true);

        var sourceSslOptions = options.getSsl();
        if (sourceSslOptions == null) {
            netClientOptions.setTrustAll(true);
            return;
        }

        var sslOptions = SslOptionsMapper.INSTANCE.map(sourceSslOptions);
        netClientOptions.setTrustAll(sslOptions.isTrustAll());
        netClientOptions.setHostnameVerificationAlgorithm(sslOptions.isHostnameVerifier() ? "HTTPS" : "");

        if (sslOptions.getTlsProtocols() != null && !sslOptions.getTlsProtocols().isEmpty()) {
            netClientOptions.setEnabledSecureTransportProtocols(sslOptions.getTlsProtocols());
        }

        if (sslOptions.getTlsCiphers() != null && !sslOptions.getTlsCiphers().isEmpty()) {
            for (String cipher : sslOptions.getTlsCiphers()) {
                netClientOptions.addEnabledCipherSuite(cipher.strip());
            }
        }

        netClientOptions.setUseAlpn(sslOptions.isAlpn());

        if (sslOptions.isOpenSsl()) {
            netClientOptions.setSslEngineOptions(new io.vertx.core.net.OpenSSLEngineOptions());
        }

        if (!sslOptions.isTrustAll()) {
            sslOptions.trustStore().flatMap(TrustStore::trustOptions).ifPresent(netClientOptions::setTrustOptions);
        }

        sslOptions.keyStore().flatMap(KeyStore::keyCertOptions).ifPresent(netClientOptions::setKeyCertOptions);
    }

    /**
     * Build the dedup key: host, port, useSsl, username, password, and sentinel
     * topology. The key is kept internal to this class and never logged.
     */
    private static String dedupKey(RedisClientOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append(options.getHost()).append(':').append(options.getPort());
        sb.append("|ssl=").append(options.isUseSsl());
        sb.append("|user=").append(nullSafe(options.getUsername()));
        sb.append("|pw=").append(nullSafe(options.getPassword()));

        RedisSentinelOptions sentinel = options.getSentinel();
        if (sentinel != null && sentinel.getNodes() != null && !sentinel.getNodes().isEmpty()) {
            sb.append("|sentinel=").append(nullSafe(sentinel.getMasterId()));
            // Sort nodes so the key is stable regardless of declaration order.
            List<HostAndPort> sortedNodes = sentinel.getNodes().stream().sorted(VertxRedisClientFactory::compareNode).toList();
            for (HostAndPort node : sortedNodes) {
                sb.append(',').append(node.getHost()).append(':').append(node.getPort());
            }
        }
        return sb.toString();
    }

    private static int compareNode(HostAndPort a, HostAndPort b) {
        int h = nullSafe(a.getHost()).compareTo(nullSafe(b.getHost()));
        return h != 0 ? h : Integer.compare(a.getPort(), b.getPort());
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Short, log-safe summary of a connection. Excludes credentials and SSL material.
     */
    private static String summary(RedisClientOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append(options.getHost()).append(':').append(options.getPort());
        sb.append(" ssl=").append(options.isUseSsl());
        RedisSentinelOptions sentinel = options.getSentinel();
        if (sentinel != null && sentinel.getNodes() != null && !sentinel.getNodes().isEmpty()) {
            sb.append(" sentinel=").append(sentinel.getMasterId()).append('(').append(sentinel.getNodes().size()).append(" nodes)");
        }
        return sb.toString();
    }

    @VisibleForTesting
    int sharedClientCount() {
        return sharedClients.size();
    }

    private record SharedRedisClient(Redis client, AtomicInteger refCount) {}
}
