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
package io.gravitee.node.opentelemetry.exporter.redact;

import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.EventData;
import java.util.ArrayList;
import java.util.List;

final class SpanAttributeRedactor {

    private final List<CompiledRedactionRule> rules;

    SpanAttributeRedactor(RedactionConfig config) {
        String defaultReplacement = config.defaultReplacement();
        this.rules = config.rules().stream().map(rule -> new CompiledRedactionRule(rule, defaultReplacement)).toList();
    }

    boolean hasRules() {
        return !rules.isEmpty();
    }

    List<EventData> redactEvents(List<EventData> events) {
        if (events.isEmpty()) {
            return events;
        }
        List<EventData> result = null;
        for (int i = 0; i < events.size(); i++) {
            EventData original = events.get(i);
            Attributes redactedAttrs = redact(original.getAttributes());
            if (redactedAttrs != original.getAttributes() && result == null) {
                result = new ArrayList<>(events.size());
                for (int j = 0; j < i; j++) {
                    result.add(events.get(j));
                }
            }
            if (result != null) {
                result.add(
                    redactedAttrs != original.getAttributes()
                        ? EventData.create(original.getEpochNanos(), original.getName(), redactedAttrs, original.getTotalAttributeCount())
                        : original
                );
            }
        }
        return result != null ? result : events;
    }

    Attributes redact(Attributes original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        var map = original.asMap();
        // Phase 1: fast scan with no allocation — return original if nothing matches
        boolean anyMatch = false;
        for (var entry : map.entrySet()) {
            if (findRule(entry.getKey().getKey(), valueAsString(entry.getValue())) != null) {
                anyMatch = true;
                break;
            }
        }
        if (!anyMatch) {
            return original;
        }
        // Phase 2: build redacted copy — allocates only when at least one rule matched
        AttributesBuilder builder = Attributes.builder();
        for (var entry : map.entrySet()) {
            String strValue = valueAsString(entry.getValue());
            CompiledRedactionRule rule = findRule(entry.getKey().getKey(), strValue);
            if (rule != null) {
                builder.put(AttributeKey.stringKey(entry.getKey().getKey()), rule.applyRedaction(strValue));
            } else {
                putRaw(builder, entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void putRaw(AttributesBuilder builder, AttributeKey<T> key, Object value) {
        builder.put(key, (T) value);
    }

    private CompiledRedactionRule findRule(String attributeKey, String attributeValue) {
        for (CompiledRedactionRule rule : rules) {
            if (rule.shouldRedact(attributeKey, attributeValue)) {
                return rule;
            }
        }
        return null;
    }

    private static String valueAsString(Object value) {
        return value != null ? value.toString() : null;
    }
}
