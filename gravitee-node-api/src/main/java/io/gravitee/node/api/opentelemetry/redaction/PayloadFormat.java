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

/**
 * The format of the payload body to be masked.
 * <p>
 * {@code AUTO} instructs the masking engine to detect the format at runtime from the
 * {@code payload.format} span-event attribute, falling back to a heuristic inspection
 * of the body string (leading {@code '{'} / {@code '['} → JSON, leading {@code '<'} → XML).
 */
public enum PayloadFormat {
    JSON,
    XML,
    AUTO,
}
