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
package io.gravitee.node.opentelemetry.configuration;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum CompressionType {
    GZIP("gzip"),
    NONE("none");

    private final String value;

    private static final Map<String, CompressionType> MAP = Map.of(NONE.value, NONE, GZIP.value, GZIP);

    public static CompressionType fromValue(final String value) {
        if (value != null && MAP.containsKey(value)) {
            return MAP.get(value);
        }
        return NONE;
    }
}
