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
 * The separator between entries defaults to a single space and can be customized.
 * </p>
 */
public final class MdcLoggingConfiguration {

    public static final String DEFAULT_FORMAT = "{key}: {value}";
    public static final String DEFAULT_SEPARATOR = " ";

    private final List<String> include;
    private final String format;
    private final String nullValue;
    private final String separator;

    public MdcLoggingConfiguration() {
        this(Collections.emptyList(), DEFAULT_FORMAT, "", DEFAULT_SEPARATOR);
    }

    public MdcLoggingConfiguration(List<String> include, String format, String nullValue, String separator) {
        this.include = include == null ? Collections.emptyList() : List.copyOf(include);
        this.format = (format == null || format.isEmpty()) ? DEFAULT_FORMAT : format;
        this.nullValue = nullValue == null ? "" : nullValue;
        this.separator = separator == null ? DEFAULT_SEPARATOR : separator;
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

    public String getSeparator() {
        return separator;
    }

    /**
     * Returns {@code true} if the given key should be included in MDC.
     * When the include list is empty, all keys are accepted.
     */
    public boolean shouldInclude(String key) {
        return include.isEmpty() || include.contains(key);
    }
}
