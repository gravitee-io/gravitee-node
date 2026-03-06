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

import java.util.Collections;
import java.util.List;

/**
 * Configuration for MDC logging behavior.
 * <p>
 * Controls which MDC keys are included in log output and how they are formatted.
 * This object is shared via an {@link java.util.concurrent.atomic.AtomicReference}
 * in {@link NodeLoggerFactory}, following the same lazy-initialization pattern
 * as the {@link io.gravitee.node.api.Node} supplier.
 * </p>
 * <p>
 * When the include list is empty (default), all MDC keys are included (backward compatibility).
 * When the include list is non-empty, only the specified keys are populated into MDC.
 * The list order is preserved, which matters for the {@code %mdcList} converter rendering.
 * </p>
 */
public final class MdcLoggingConfiguration {

    private final List<String> include;
    private final String format;
    private final String nullValue;
    private final boolean filterAll;

    public MdcLoggingConfiguration() {
        this(Collections.emptyList(), "%s=\"%s\"", "", false);
    }

    public MdcLoggingConfiguration(List<String> include, String format, String nullValue) {
        this(include, format, nullValue, false);
    }

    public MdcLoggingConfiguration(List<String> include, String format, String nullValue, boolean filterAll) {
        this.include = include == null ? Collections.emptyList() : List.copyOf(include);
        this.format = (format == null || format.isEmpty()) ? "%s=\"%s\"" : format;
        this.nullValue = nullValue == null ? "" : nullValue;
        this.filterAll = filterAll;
    }

    public List<String> getInclude() {
        return include;
    }

    public String getFormat() {
        return format;
    }

    public String getNullValue() {
        return nullValue;
    }

    /**
     * Returns {@code true} if the given key should be included in MDC.
     * When the include list is empty, all keys are accepted.
     */
    public boolean shouldInclude(String key) {
        return include.isEmpty() || include.contains(key);
    }

    /**
     * Returns {@code true} if MDC keys set outside of {@link NodeAwareLogger}
     * (e.g., via direct {@code MDC.put()} calls) should also be filtered
     * according to the include list.
     */
    public boolean isFilterAll() {
        return filterAll;
    }
}
