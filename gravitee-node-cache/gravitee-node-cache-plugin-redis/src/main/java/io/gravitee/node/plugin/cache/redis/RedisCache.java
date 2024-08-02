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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class RedisCache<V> implements Cache<String, V> {

    public static final String SEPARATOR = ".";
    private final String name;
    private final ValueMapper<V, String> valueMapper;
    private final RedisAPI redisAPI;
    private final Map<String, CacheListener<String, V>> cacheListeners = new HashMap<>();

    public RedisCache(String name, RedisAPI redisAPI, ValueMapper<V, String> mapper) {
        this.name = name;
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
        return Single
            .fromCompletionStage(this.redisAPI.keys(getRedisEntryKey("*")).toCompletionStage())
            .flatMap(this::throwExceptionOnError)
            .flattenStreamAsFlowable(Response::stream)
            .map(response -> response.toString(StandardCharsets.UTF_8).replace(getRedisEntryKey(""), ""));
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
        return Single
            .fromCompletionStage(this.redisAPI.exists(List.of(key + SEPARATOR)).toCompletionStage())
            .flatMap(this::throwExceptionOnError)
            .map(resp -> resp.toInteger() > 0);
    }

    @Override
    public V get(String key) {
        return this.rxGet(key).blockingGet();
    }

    @Override
    public Maybe<V> rxGet(String key) {
        return Maybe
            .fromCompletionStage(this.redisAPI.get(getRedisEntryKey(key)).toCompletionStage())
            .flatMapSingle(this::throwExceptionOnError)
            .map(response -> valueMapper.toValue(asString(response)));
    }

    @Override
    public V put(String key, V value) {
        return this.rxPut(key, value).blockingGet();
    }

    @Override
    public Maybe<V> rxPut(String key, V value) {
        return rxGet(key)
            .map(Optional::ofNullable)
            .switchIfEmpty(Maybe.just(Optional.empty()))
            .flatMap(oldValue ->
                Maybe
                    .fromCompletionStage(this.redisAPI.setnx(getRedisEntryKey(key), valueMapper.toCachedValue(value)).toCompletionStage())
                    .map(this::throwExceptionOnError)
                    .mapOptional(r -> {
                        this.cacheListeners.values()
                            .forEach(listener ->
                                oldValue.ifPresentOrElse(
                                    old -> listener.onEntryUpdated(key, old, value),
                                    () -> listener.onEntryAdded(key, value)
                                )
                            );
                        return oldValue;
                    })
            );
    }

    @Override
    public V put(String key, V value, long ttl, TimeUnit ttlUnit) {
        return this.rxPut(key, value, ttl, ttlUnit).blockingGet();
    }

    @Override
    public Maybe<V> rxPut(String key, V value, long ttl, TimeUnit ttlUnit) {
        return rxGet(key)
            .map(Optional::ofNullable)
            .switchIfEmpty(Maybe.just(Optional.empty()))
            .flatMap(oldValue ->
                Maybe
                    .fromCompletionStage(
                        this.redisAPI.set(
                                List.of(getRedisEntryKey(key), valueMapper.toCachedValue(value), "PX", "" + ttlUnit.toMillis(ttl))
                            )
                            .toCompletionStage()
                    )
                    .map(this::throwExceptionOnError)
                    .mapOptional(r -> {
                        this.cacheListeners.values()
                            .forEach(listener ->
                                oldValue.ifPresentOrElse(
                                    old -> listener.onEntryUpdated(key, old, value),
                                    () -> listener.onEntryAdded(key, value)
                                )
                            );
                        return oldValue;
                    })
            );
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        this.rxPutAll(m).blockingAwait();
    }

    @Override
    public Completable rxPutAll(Map<? extends String, ? extends V> m) {
        return Flowable
            .fromIterable(m.entrySet())
            .flatMapCompletable(entry -> this.rxPut(entry.getKey(), entry.getValue()).ignoreElement());
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
        return this.rxGet(key)
            .flatMap(value ->
                Maybe
                    .fromCompletionStage(this.redisAPI.del(List.of(getRedisEntryKey(key))).toCompletionStage())
                    .map(this::throwExceptionOnError)
                    .map(ignore -> {
                        this.cacheListeners.values().forEach(listener -> listener.onEntryEvicted(key, value));
                        return value;
                    })
            );
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

    private static String asString(Response response) {
        return response.toString(StandardCharsets.UTF_8);
    }
}
