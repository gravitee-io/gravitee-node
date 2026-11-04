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
import io.gravitee.node.api.cache.CacheListener;
import io.gravitee.node.api.cache.ValueMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RedisCache<V> implements Cache<String, V> {

    public static final String SEPARATOR = ".";

    /**
     * Write the value and return the previous one in a single, atomic step.
     * Lua is used rather than {@code SET ... GET} so that Redis versions older than 6.2 keep working.
     * KEYS[1]: entry key, ARGV[1]: value, ARGV[2]: time to live in milliseconds, 0 for none.
     */
    private static final String PUT_SCRIPT =
        "local old = redis.call('GET', KEYS[1]) " +
        "if tonumber(ARGV[2]) > 0 then redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
        "else redis.call('SET', KEYS[1], ARGV[1]) end " +
        "return old";

    /**
     * Remove the entry and return its value in a single, atomic step (same as {@code GETDEL}, Redis 6.2+).
     * KEYS[1]: entry key.
     */
    private static final String EVICT_SCRIPT =
        "local old = redis.call('GET', KEYS[1]) if old then redis.call('DEL', KEYS[1]) end return old";

    private final String name;
    private final CacheConfiguration configuration;
    private final ValueMapper<V, String> valueMapper;
    private final RedisAPI redisAPI;
    private final Map<String, CacheListener<String, V>> cacheListeners = new HashMap<>();

    public RedisCache(String name, RedisAPI redisAPI, ValueMapper<V, String> mapper) {
        this(name, new CacheConfiguration(), redisAPI, mapper);
    }

    public RedisCache(String name, CacheConfiguration configuration, RedisAPI redisAPI, ValueMapper<V, String> mapper) {
        this.name = name;
        this.configuration = configuration;
        this.redisAPI = redisAPI;
        if (mapper == null) {
            throw new IllegalArgumentException("ValueMapper required for Redis Cache");
        }
        this.valueMapper = mapper;
    }

    @Override
    public String getName() {
        return this.name;
    }

    private String getRedisEntryKey(String key) {
        return this.name + SEPARATOR + key;
    }

    private Single<Response> throwExceptionOnError(Response response) {
        if (ResponseType.ERROR == response.type()) {
            return Single.error(new CacheException("Error during cache operation: " + response.format()));
        }
        return Single.just(response);
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("Redis cache does not support values operation");
    }

    @Override
    public Flowable<String> rxKeys() {
        return Single.fromCompletionStage(this.redisAPI.keys(getRedisEntryKey("*")).toCompletionStage())
            .flatMap(this::throwExceptionOnError)
            .flattenStreamAsFlowable(Response::stream)
            .map(response -> response.toString(StandardCharsets.UTF_8).replace(getRedisEntryKey(""), ""))
            .onErrorResumeNext(ex -> Flowable.error(new CacheException("Keys cannot be listed from cache", ex)));
    }

    @Override
    public Set<String> keys() {
        return this.rxKeys().collectInto(new HashSet<String>(), HashSet::add).blockingGet();
    }

    @Override
    public Set<Map.Entry<String, V>> entrySet() {
        throw new UnsupportedOperationException("Redis cache does not support entrySet operation");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Redis cache does not support size operation");
    }

    @Override
    public boolean isEmpty() {
        return this.rxIsEmpty().blockingGet();
    }

    @Override
    public Single<Boolean> rxIsEmpty() {
        return this.rxKeys().isEmpty();
    }

    @Override
    public boolean containsKey(String key) {
        return this.rxContainsKey(key).blockingGet();
    }

    @Override
    public Single<Boolean> rxContainsKey(String key) {
        return Single.fromCompletionStage(this.redisAPI.exists(List.of(getRedisEntryKey(key))).toCompletionStage())
            .flatMap(this::throwExceptionOnError)
            .map(resp -> resp.toInteger() > 0)
            .onErrorResumeNext(ex -> Single.error(new CacheException("Key existence cannot be tested from cache", ex)));
    }

    @Override
    public V get(String key) {
        return this.rxGet(key).blockingGet();
    }

    @Override
    public Maybe<V> rxGet(String key) {
        return Maybe.fromCompletionStage(this.redisAPI.get(getRedisEntryKey(key)).toCompletionStage())
            .flatMapSingle(this::throwExceptionOnError)
            .map(response -> valueMapper.toValue(asString(response)))
            .onErrorResumeNext(ex -> Maybe.error(new CacheException("Keys cannot be listed from cache", ex)));
    }

    @Override
    public V put(String key, V value) {
        return this.rxPut(key, value).blockingGet();
    }

    @Override
    public Maybe<V> rxPut(String key, V value) {
        return put(key, value, 0);
    }

    @Override
    public V put(String key, V value, long ttl, TimeUnit ttlUnit) {
        return this.rxPut(key, value, ttl, ttlUnit).blockingGet();
    }

    @Override
    public Maybe<V> rxPut(String key, V value, long ttl, TimeUnit ttlUnit) {
        return put(key, value, ttlUnit.toMillis(ttl));
    }

    private Maybe<V> put(String key, V value, long ttlInMs) {
        final long effectiveTtlInMs;
        try {
            effectiveTtlInMs = effectiveTimeToLive(ttlInMs);
        } catch (IllegalArgumentException e) {
            return Maybe.error(e);
        }
        return Maybe.fromCompletionStage(
            this.redisAPI.eval(
                List.of(PUT_SCRIPT, "1", getRedisEntryKey(key), valueMapper.toCachedValue(value), Long.toString(effectiveTtlInMs))
            ).toCompletionStage()
        )
            .map(response -> Optional.ofNullable(valueMapper.toValue(asString(response))))
            .defaultIfEmpty(Optional.empty())
            .flatMapMaybe(oldValue -> {
                this.cacheListeners.values().forEach(listener ->
                    oldValue.ifPresentOrElse(old -> listener.onEntryUpdated(key, old, value), () -> listener.onEntryAdded(key, value))
                );
                return Maybe.fromOptional(oldValue);
            })
            .onErrorResumeNext(ex -> Maybe.error(new CacheException("Unable to add cache Entry", ex)));
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        this.rxPutAll(m).blockingAwait();
    }

    @Override
    public Completable rxPutAll(Map<? extends String, ? extends V> m) {
        return Flowable.fromIterable(m.entrySet()).flatMapCompletable(entry ->
            this.rxPut(entry.getKey(), entry.getValue()).ignoreElement()
        );
    }

    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> remappingFunction) {
        return this.rxComputeIfAbsent(key, remappingFunction).blockingGet();
    }

    @Override
    public Maybe<V> rxComputeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        throw new UnsupportedOperationException("Redis cache does not support computeIfAbsent operation");
    }

    @Override
    public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return this.rxComputeIfPresent(key, remappingFunction).blockingGet();
    }

    @Override
    public Maybe<V> rxComputeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException("Redis cache does not support computeIfPresent operation");
    }

    @Override
    public V compute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return this.rxCompute(key, remappingFunction).blockingGet();
    }

    @Override
    public Maybe<V> rxCompute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException("Redis cache does not support compute operation");
    }

    @Override
    public V evict(String key) {
        return this.rxEvict(key).blockingGet();
    }

    @Override
    public Maybe<V> rxEvict(String key) {
        return Maybe.fromCompletionStage(this.redisAPI.eval(List.of(EVICT_SCRIPT, "1", getRedisEntryKey(key))).toCompletionStage())
            .map(response -> valueMapper.toValue(asString(response)))
            .doOnSuccess(value -> this.cacheListeners.values().forEach(listener -> listener.onEntryEvicted(key, value)))
            .onErrorResumeNext(ex -> Maybe.error(new CacheException("Unable to remove cache Entry", ex)));
    }

    @Override
    public void clear() {
        this.rxClear().blockingAwait();
    }

    @Override
    public Completable rxClear() {
        return this.rxKeys().flatMapCompletable(key -> rxEvict(key).ignoreElement());
    }

    @Override
    public String addCacheListener(CacheListener<String, V> listener) {
        String listenerCacheId = io.gravitee.common.utils.UUID.random().toString();
        cacheListeners.put(listenerCacheId, listener);

        return listenerCacheId;
    }

    @Override
    public boolean removeCacheListener(String listenerCacheId) {
        return cacheListeners.remove(listenerCacheId) != null;
    }

    /**
     * An entry written without its own time to live gets the one from the configuration, as in the other caches.
     * Its own time to live can't be bigger than the configured one.
     */
    private long effectiveTimeToLive(long ttlInMs) {
        long configuredTtlInMs = configuration.getTimeToLiveInMs();
        if (ttlInMs > 0) {
            if (configuredTtlInMs > 0 && configuredTtlInMs < ttlInMs) {
                throw new IllegalArgumentException("TTL can't be bigger than ttl defined in the cache configuration");
            }
            return ttlInMs;
        }
        return Math.max(configuredTtlInMs, 0);
    }

    private static String asString(Response response) {
        return response.toString(StandardCharsets.UTF_8);
    }
}
