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

/**
 * Some caching system only accept String or primitive values so java object can't be stored.
 * This interface allows to provide to the cache implementation a mapper to convert the value provided to the
 * cache object in a type accepted by the caching system and in the same way convert the cached value to a type
 * expected by the application.
 *
 * @param <V> the value type provided by the application
 * @param <MV> the value type accepted by the caching system
 */
public interface ValueMapper<V, MV> {
    MV toCachedValue(V value);

    V toValue(MV cachedValue);
}
