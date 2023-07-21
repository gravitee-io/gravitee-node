/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.secrets.service.resolver;

import io.reactivex.rxjava3.core.Flowable;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public interface WatchablePropertyResolver<T> extends PropertyResolver<T> {
    /**
     * Check if this property can be watched
     *
     * @param value the property value
     * @return true if revolve or watch can be called
     */
    default boolean isWatchable(String value) {
        return true;
    }

    /**
     * Watch for any changes in the property and emmit the new values
     *
     * @param location the value as a URL
     * @return a Flowable of resolved value
     */
    Flowable<T> watch(String location);
}
