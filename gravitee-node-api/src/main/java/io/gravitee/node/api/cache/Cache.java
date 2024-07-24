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
    /**
     * Get the name of this cache.
     *
     * @return the name if this cache.
     */
    String getName();

    /**
     * Get a collection of all the values in this cache.
     *
     * @return a collection of all the values in the cache.
     */
    Collection<V> values();

    /**
     * Reactive method to get all the values in this cache as a <code>Flowable</code>.
     *
     * @return a <code>Flowable</code> containing all the values in the cache.
     */
    default Flowable<V> rxValues() {
        return Flowable.fromIterable(this.values()).subscribeOn(Schedulers.io());
    }

    /**
     * Get a set of all the keys in this cache.
     *
     * @return a set of all the keys in the cache.
     */
    Set<K> keys();

    /**
     * Reactive method to get all the keys in this cache as a <code>Flowable</code>.
     *
     * @return a <code>Flowable</code> containing all the keys in the cache.
     */
    default Flowable<K> rxKeys() {
        return Flowable.fromIterable(this.keys()).subscribeOn(Schedulers.io());
    }

    /**
     * Get set of all the entries in the cache.
     *
     * @return all the entries in the cache.
     */
    Set<Map.Entry<K, V>> entrySet();

    /**
     * Reactive method to get all the entries in this cache as a <code>Flowable</code>.
     *
     * @return a <code>Flowable</code> containing all the entries in the cache.
     */
    default Flowable<Map.Entry<K, V>> rxEntrySet() {
        return Flowable.fromIterable(this.entrySet()).subscribeOn(Schedulers.io());
    }

    /**
     * Get the current size of this cache.
     *
     * @return the size of the cache.
     */
    int size();

    /**
     * Reactive method to get the current size of this cache.
     *
     * @return a <code>Single</code> containing the size of the cache.
     */
    default Single<Integer> rxSize() {
        return Single.fromCallable(this::size).subscribeOn(Schedulers.io());
    }

    /**
     * Check if this cache is empty.
     *
     * @return <code>true</code> if the cache is empty, <code>false</code> otherwise.
     */
    boolean isEmpty();

    /**
     * Reactive method to check if this cache is empty.
     *
     * @return a <code>Single<code> containing <code>true</code> if the cache is empty, <code>false</code> otherwise.
     */
    default Single<Boolean> rxIsEmpty() {
        return Single.fromCallable(this::isEmpty).subscribeOn(Schedulers.io());
    }

    /**
     * Check if the specified key is in this cache.
     * @param key the key.
     *
     * @return <code>true</code> if the key is in the cache, <code>false</code> otherwise.
     */
    boolean containsKey(final K key);

    /**
     * Reactive method to check if the specified key is in this cache.
     * @param key the key.
     *
     * @return a <code>Single<code> containing <code>true</code> if the key is in the cache, <code>false</code> otherwise.
     */
    default Single<Boolean> rxContainsKey(final K key) {
        return Single.fromCallable(() -> this.containsKey(key)).subscribeOn(Schedulers.io());
    }

    /**
     * Return the value for the specified key or <code>null</code> if this cache does not contain this key.
     * @param key the key used to get the value.
     *
     * @return the value or <code>null</code> if no value has been found.
     */
    V get(final K key);

    /**
     * Reactive method to get the value for the specified key as a <code>Maybe</code>. An empty <code>Maybe</code> is returned if this cache does not contain this key.
     * @param key the key used to get the value.
     *
     * @return a <code>Maybe</code> containing the value or an empty <code>Maybe</code> if no value has been found.
     */
    default Maybe<V> rxGet(final K key) {
        return Maybe.fromCallable(() -> this.get(key)).subscribeOn(Schedulers.io());
    }

    /**
     * Associate the value to the specified key in this cache and returns the previous value that was associated to this key or <code>null</code>
     * if no value was previously associated to this key.
     * @param key the key.
     * @param value the value.
     *
     * @return the previous value associated to the specified key or <code>null</code> if no value was present.
     */
    V put(final K key, final V value);

    /**
     * Reactive method to associate the value to the specified key in this cache and return a <code>Maybe</code> containing the previous value that was associated to this key.
     * An empty <code>Maybe</code> is returned if no value was previously associated to this key.
     * @param key the key.
     * @param value the value.
     *
     * @return a <code>Maybe</code> containing the previous value or an empty <code>Maybe</code> if no value was present.
     */
    default Maybe<V> rxPut(final K key, final V value) {
        return Maybe.fromCallable(() -> this.put(key, value)).subscribeOn(Schedulers.io());
    }

    /**
     * Same as {@link #put(Object, Object)} but with a time to live.
     * @param key the key.
     * @param value the value.
     *
     * @return the previous value associated to the specified key or <code>null</code> if no value was present.
     */
    V put(final K key, final V value, final long ttl, final TimeUnit ttlUnit);

    /**
     * Same as {@link #rxPut(Object, Object)} but with a time to live.
     * @param key the key.
     * @param value the value.
     *
     * @return the previous value associated to the specified key or <code>null</code> if no value was present.
     */
    default Maybe<V> rxPut(final K key, final V value, final long ttl, final TimeUnit ttlUnit) {
        return Maybe.fromCallable(() -> this.put(key, value, ttl, ttlUnit)).subscribeOn(Schedulers.io());
    }

    /**
     * Put all the specified key/value pairs into this cache.
     * @param m a map containing all the key/value pairs to put into this cache.
     */
    void putAll(final Map<? extends K, ? extends V> m);

    /**
     * Reactive method to put all the specified key/value pairs into this cache.
     * @param m a map containing all the key/value pairs to put into this cache.
     *
     * @return a <code>Completable</code> that completes once all the key/value pairs have been put in the cache.
     */
    default Completable rxPutAll(final Map<? extends K, ? extends V> m) {
        return Completable.fromRunnable(() -> this.putAll(m)).subscribeOn(Schedulers.io());
    }

    /**
     * If the specified key is not already associated with a value, attempt to compute the value of the specified key using the given remapping function.
     * If the remapping function returns <code>null</code>, the mapping is removed.
     * @param key the key.
     * @param remappingFunction the function that is executed to associate the new value in case no value was present.
     *
     * @return the computed value if it was absent (<code>null</code> if the mapping function returns <code>null</code>) or the existing one if it was already present.
     */
    V computeIfAbsent(K key, Function<? super K, ? extends V> remappingFunction);

    /**
     * Reactive method to compute the value of the specified key using the given mapping function if it is not already associated with a value.
     * If the remapping function returns <code>null</code>, the mapping is removed.
     * @param key the key.
     * @param mappingFunction the function that is executed to associate the new value in case no value was present.
     *
     * @return a <code>Maybe</code> containing the computed value if it was absent (empty if the mapping function returns <code>null</code>) or the existing one if it was already present.
     */
    default Maybe<V> rxComputeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return Maybe.fromCallable(() -> this.computeIfAbsent(key, mappingFunction)).subscribeOn(Schedulers.io());
    }

    /**
     * If the specified key is already associated with a value, attempt to compute the value of the specified key using the given mapping function.
     * If the remapping function returns <code>null</code>, the mapping is removed.
     * @param key the key.
     * @param remappingFunction the function that is executed to associate the new value in case a value was present.
     *
     * @return the computed value (<code>null</code> if the mapping function returns <code>null</code>).
     */
    V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Reactive method to compute the value of the specified key using the given mapping function if it is already associated with a value.
     * If the remapping function returns <code>null</code>, the mapping is removed.
     * @param key the key.
     * @param remappingFunction the function that is executed to associate the new value in case a value was present.
     *
     * @return a <code>Maybe</code> containing the computed value (empty if the mapping function returns <code>null</code>).
     */
    default Maybe<V> rxComputeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Maybe.fromCallable(() -> this.computeIfPresent(key, remappingFunction)).subscribeOn(Schedulers.io());
    }

    /**
     * Compute the value of the specified key using the given mapping function.
     * If the remapping function returns <code>null</code>, the mapping is removed.
     * @param key the key.
     * @param remappingFunction the function that is executed to associate the new value.
     *
     * @return the computed value (<code>null</code> if the mapping function returns <code>null</code>).
     */
    V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Reactive method to compute the value of the specified key using the given mapping function.
     * @param key the key.
     * @param remappingFunction the function that is executed to associate the new value.
     *
     * @return a <code>Maybe</code> containing the computed value (empty one if the mapping function returns <code>null</code>).
     */
    default Maybe<V> rxCompute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Maybe.fromCallable(() -> this.compute(key, remappingFunction)).subscribeOn(Schedulers.io());
    }

    /**
     * Remove the key/value entry from the cache.
     * @param key the key.
     *
     * @return the remove value or <code>null</code> if it wasn't present.
     */
    V evict(final K key);

    /**
     * Reactive method to remove the key/value entry from the cache.
     * @param key the key.
     *
     * @return a <code>Maybe</code> containing the removed value or an empty one if it wasn't present.
     */
    default Maybe<V> rxEvict(final K key) {
        return Maybe.fromCallable(() -> this.evict(key)).subscribeOn(Schedulers.io());
    }

    /**
     * Clear the cache.
     */
    void clear();

    /**
     * Reactive method to clear the cache.
     *
     * @return a <code>Completable</code> that completes when the cache has been cleared.
     */
    default Completable rxClear() {
        return Completable.fromRunnable(this::clear).subscribeOn(Schedulers.io());
    }

    /**
     * Add a listener that will be called on for each action on this cache.
     * @param listener the cache listener.
     *
     * @return an uuid for this cache listener that can be used to remove this it.
     */
    String addCacheListener(CacheListener<K, V> listener);

    /**
     * Reactive method to add a listener that will be called on for each action on this cache.
     * @param listener the cache listener.
     *
     * @return a <code>Single</code> containing an uuid for this cache listener that can be used to remove this it.
     */
    default Single<String> rxAddCacheListener(CacheListener<K, V> listener) {
        return Single.fromCallable(() -> this.addCacheListener(listener)).subscribeOn(Schedulers.io());
    }

    /**
     * Remove the specified cache listener.
     * @param listenerCacheId the id of the cache listener to remove.
     *
     * @return <code>true</code> if the cache listener has been removed, <code>false</code> otherwise.
     */
    boolean removeCacheListener(final String listenerCacheId);

    /**
     * Reactive method to remove the specified cache listener.
     * @param listenerCacheId the id of the cache listener to remove.
     *
     * @return a <code>Single</code> containing <code>true</code> if the cache listener has been remove or <code>false</code> otherwise.
     */
    default Single<Boolean> rxRemoveCacheListener(final String listenerCacheId) {
        return Single.fromCallable(() -> this.removeCacheListener(listenerCacheId)).subscribeOn(Schedulers.io());
    }
}
