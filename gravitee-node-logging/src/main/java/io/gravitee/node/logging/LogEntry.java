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
package io.gravitee.node.logging;

import java.util.Objects;

/**
 * Generic LogEntry contract to harmonize MDC key/value extraction across contexts.
 *
 * @param <T> Context type used to extract the value.
 */
public interface LogEntry<T> {
    /**
     * Retrieves the key associated with the log entry.
     *
     * @return the key as a string.
     */
    String getKey();

    /**
     * Retrieves the value associated with the log entry by extracting it from the provided value source.
     *
     * @param valueSource the value source from which the value is extracted.
     * @return the extracted value as a string, or a default value if extraction fails.
     */
    String getValue(T valueSource);

    /**
     * Returns the runtime type of the value source required by this entry.
     * This allows loggers to route the correct source instance without
     * knowing concrete types or keys.
     */
    Class<T> sourceType();

    /**
     * Resets the state of the log entry.
     * This operation clears any cached or previously computed data,
     * allowing the log entry to be re-evaluated or reused in its initial state.
     * This is mainly intented for test purposes.
     */
    void reset();

    /**
     * Convenience method to resolve the value from an untyped source by
     * safely casting it using {@link #sourceType()}.
     */
    default String resolve(Object source) {
        return getValue(sourceType().cast(source));
    }
}
