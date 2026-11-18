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
package io.gravitee.node.api.license.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Declares an attribute that tiers and licenses may set.
 *
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseAttribute {

    private Type type;

    /**
     * A limit can only be raised by a license: the higher of the tier's and the license's value applies.
     */
    private boolean limit;

    public enum Type {
        @JsonProperty("integer")
        INTEGER,
        @JsonProperty("string")
        STRING;

        public boolean accepts(Object value) {
            return switch (this) {
                case INTEGER -> value instanceof Integer || value instanceof Long;
                case STRING -> value instanceof String;
            };
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
