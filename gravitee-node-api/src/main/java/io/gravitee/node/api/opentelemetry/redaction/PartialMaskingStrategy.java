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
package io.gravitee.node.api.opentelemetry.redaction;

public record PartialMaskingStrategy(int prefixLength, int suffixLength, String maskChar) implements MaskingStrategy {
    public PartialMaskingStrategy {
        if (maskChar == null || maskChar.isEmpty()) {
            maskChar = "*";
        }
        if (maskChar.length() > 1) {
            throw new IllegalArgumentException("PARTIAL mask character must be exactly one character, got: \"" + maskChar + "\"");
        }
        if (prefixLength < 0) throw new IllegalArgumentException("prefixLength must be >= 0, got: " + prefixLength);
        if (suffixLength < 0) throw new IllegalArgumentException("suffixLength must be >= 0, got: " + suffixLength);
    }
}
