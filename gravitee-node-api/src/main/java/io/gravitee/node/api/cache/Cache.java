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
package io.gravitee.node.api.cache;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Cache<K, V> {
    String getName();

    Collection<V> values();

    default Flowable<V> rxValues() {
        return Flowable.fromIterable(this.values()).subscribeOn(Schedulers.io());
    }

    Set<K> keys();

    default Flowable<K> rxKeys() {
        return Flowable.fromIterable(this.keys()).subscribeOn(Schedulers.io());
    }

    Set<Map.Entry<K, V>> entrySet();

    default Flowable<Map.Entry<K, V>> rxEntrySet() {
        return Flowable.fromIterable(this.entrySet()).subscribeOn(Schedulers.io());
    }

    int size();

    default Single<Integer> rxSize() {
        return Single.fromCallable(this::size).subscribeOn(Schedulers.io());
    }

    boolean isEmpty();

    default Single<Boolean> rxIsEmpty() {
        return Single.fromCallable(this::isEmpty).subscribeOn(Schedulers.io());
    }

    boolean containsKey(final K key);

    default Single<Boolean> rxContainsKey(final K key) {
        return Single.fromCallable(() -> this.containsKey(key)).subscribeOn(Schedulers.io());
    }

    V get(final K key);

    default Maybe<V> rxGet(final K key) {
        return Maybe.fromCallable(() -> this.get(key)).subscribeOn(Schedulers.io());
    }

    V put(final K key, final V value);

    default Single<V> rxPut(final K key, final V value) {
        return Single.fromCallable(() -> this.put(key, value)).subscribeOn(Schedulers.io());
    }

    V put(final K key, final V value, final long ttl, final TimeUnit ttlUnit);

    default Single<V> rxPut(final K key, final V value, final long ttl, final TimeUnit ttlUnit) {
        return Single.fromCallable(() -> this.put(key, value, ttl, ttlUnit)).subscribeOn(Schedulers.io());
    }

    void putAll(final Map<? extends K, ? extends V> m);

    default Completable rxPutAll(final Map<? extends K, ? extends V> m) {
        return Completable.fromRunnable(() -> this.putAll(m)).subscribeOn(Schedulers.io());
    }

    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    default Single<V> rxComputeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return Single.fromCallable(() -> this.computeIfAbsent(key, mappingFunction)).subscribeOn(Schedulers.io());
    }

    V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    default Single<V> rxComputeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Single.fromCallable(() -> this.computeIfPresent(key, remappingFunction)).subscribeOn(Schedulers.io());
    }

    V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    default Single<V> rxCompute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Single.fromCallable(() -> this.compute(key, remappingFunction)).subscribeOn(Schedulers.io());
    }

    V evict(final K key);

    default Single<V> rxEvict(final K key) {
        return Single.fromCallable(() -> this.evict(key)).subscribeOn(Schedulers.io());
    }

    void clear();

    default Completable rxClear() {
        return Completable.fromRunnable(this::clear).subscribeOn(Schedulers.io());
    }

    String addCacheListener(CacheListener<K, V> listener);

    default Single<String> rxAddCacheListener(CacheListener<K, V> listener) {
        return Single.fromCallable(() -> this.addCacheListener(listener)).subscribeOn(Schedulers.io());
    }

    boolean removeCacheListener(final String listenerCacheId);

    default Completable rxRemoveCacheListener(final String listenerCacheId) {
        return Completable.fromRunnable(() -> this.removeCacheListener(listenerCacheId)).subscribeOn(Schedulers.io());
    }
}
