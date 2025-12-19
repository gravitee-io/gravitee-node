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

import java.util.function.Function;
import lombok.AllArgsConstructor;

/**
 * Factory to create LogEntry instances.
 * Provides cached (one-shot evaluation) and refreshable (evaluate at each call) variants.
 */
public final class LogEntryFactory {

    public static final String TO_STRING_PATTERN = "%s{key='%s', type=%s}";

    private LogEntryFactory() {}

    /**
     * Creates a cached {@link LogEntry} instance that evaluates the given extractor function only once
     * for a specific context and caches the result for future retrievals.
     *
     * @param <T> The context type used to extract the value.
     * @param key The key associated with the log entry.
     * @param extractor The function used to extract the value from the provided context.
     * @return A cached {@link LogEntry} instance that holds the key and the cached value.
     */
    public static <T> LogEntry<T> cached(String key, Class<T> sourceType, Function<? super T, String> extractor) {
        return new CachedLogEntry<>(key, sourceType, extractor);
    }

    /**
     * Creates a refreshable {@link LogEntry} instance that evaluates the given extractor function at each retrieval.
     * Each call to {@code get} on the returned {@link LogEntry} re-evaluates the extractor function with the provided context.
     *
     * @param <T> The context type used to extract the value.
     * @param key The key associated with the log entry.
     * @param extractor The function used to extract the value from the provided context.
     * @return A refreshable {@link LogEntry} instance that holds the key and re-evaluates the value at each call.
     */
    public static <T> LogEntry<T> refreshable(String key, Class<T> sourceType, Function<? super T, String> extractor) {
        return new RefreshableLogEntry<>(key, sourceType, extractor);
    }

    /**
     * Creates a fixed {@link LogEntry} instance with a predefined key and value.
     * The returned {@link LogEntry} always returns the same value regardless of context.
     *
     * @param key The key associated with the log entry.
     * @param value The constant value associated with the log entry.
     * @return A fixed {@link LogEntry} instance holding the specified key and value.
     */
    public static LogEntry<?> fixed(String key, String value) {
        return new FixedLogEntry(key, value);
    }

    private static final class CachedLogEntry<T> implements LogEntry<T> {

        private final String key;
        private final Class<T> type;
        private final Function<? super T, String> extractor;
        private boolean evaluated = false;
        private String value = null;

        private CachedLogEntry(String key, Class<T> type, Function<? super T, String> extractor) {
            this.key = key;
            this.type = type;
            this.extractor = extractor;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Class<T> sourceType() {
            return type;
        }

        @Override
        public String getValue(T ctx) {
            if (!evaluated || value == null) {
                value = extractor.apply(ctx);
                evaluated = true;
            }
            return value;
        }

        @Override
        public void reset() {
            evaluated = false;
            value = null;
        }

        @Override
        public String toString() {
            return String.format(TO_STRING_PATTERN, "CachedLogEntry", key, type);
        }
    }

    @AllArgsConstructor
    private static final class RefreshableLogEntry<T> implements LogEntry<T> {

        private final String key;
        private final Class<T> type;
        private final Function<? super T, String> extractor;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Class<T> sourceType() {
            return type;
        }

        @Override
        public String getValue(T ctx) {
            return extractor.apply(ctx);
        }

        @Override
        public void reset() {
            // Do nothing
        }

        @Override
        public String toString() {
            return String.format(TO_STRING_PATTERN, "RefreshableLogEntry", key, type);
        }
    }

    @AllArgsConstructor
    private static final class FixedLogEntry implements LogEntry<Object> {

        private String key;
        private String value;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Class<Object> sourceType() {
            return Object.class;
        }

        @Override
        public String getValue(Object valueSource) {
            return value;
        }

        @Override
        public void reset() {
            // Do nothing
        }

        @Override
        public String toString() {
            return String.format("%s{key='%s', value=%s}", "FixedLogEntry", key, value);
        }
    }
}
