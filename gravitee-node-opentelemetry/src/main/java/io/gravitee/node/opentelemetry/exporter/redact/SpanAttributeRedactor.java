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

import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingConfig;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.EventData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;

@CustomLog
final class SpanAttributeRedactor {

    private static final String PAYLOAD_EVENT_NAME = "payload";
    private static final AttributeKey<String> PAYLOAD_BODY = AttributeKey.stringKey("payload.body");
    private static final AttributeKey<String> PAYLOAD_FORMAT = AttributeKey.stringKey("payload.format");
    private static final AttributeKey<String> PAYLOAD_PHASE = AttributeKey.stringKey("payload.phase");

    private final List<CompiledRedactionRule> rules;
    private final PayloadFieldRedactor payloadRedactor;

    SpanAttributeRedactor(RedactionConfig config) {
        this(config, PayloadMaskingConfig.EMPTY);
    }

    SpanAttributeRedactor(RedactionConfig redactionConfig, PayloadMaskingConfig payloadMaskingConfig) {
        String defaultReplacement = redactionConfig.defaultReplacement();
        this.rules = redactionConfig.rules().stream().map(rule -> new CompiledRedactionRule(rule, defaultReplacement)).toList();
        this.payloadRedactor = payloadMaskingConfig.hasRules() ? new PayloadFieldRedactor(payloadMaskingConfig) : null;
    }

    boolean hasRules() {
        return !rules.isEmpty() || payloadRedactor != null;
    }

    List<EventData> redactEvents(List<EventData> events) {
        if (events.isEmpty()) {
            return events;
        }
        List<EventData> result = null;
        for (int i = 0; i < events.size(); i++) {
            EventData original = events.get(i);
            Attributes redactedAttrs = redact(original.getAttributes());
            redactedAttrs = redactPayloadBody(original.getName(), redactedAttrs);
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

    private Attributes redactPayloadBody(String eventName, Attributes attrs) {
        if (payloadRedactor == null || !PAYLOAD_EVENT_NAME.equals(eventName)) {
            return attrs;
        }
        Map<AttributeKey<?>, Object> attrMap = attrs.asMap();
        String body = (String) attrMap.get(PAYLOAD_BODY);
        if (body == null) {
            return attrs;
        }
        String format = (String) attrMap.get(PAYLOAD_FORMAT);
        String phaseStr = (String) attrMap.get(PAYLOAD_PHASE);
        PayloadPhase phase;
        if (phaseStr == null || "REQUEST".equalsIgnoreCase(phaseStr)) {
            phase = PayloadPhase.REQUEST;
        } else if ("RESPONSE".equalsIgnoreCase(phaseStr)) {
            phase = PayloadPhase.RESPONSE;
        } else {
            log.warn("PayloadMasking: unrecognised payload.phase value '{}' — defaulting to REQUEST", phaseStr);
            phase = PayloadPhase.REQUEST;
        }
        String maskedBody = payloadRedactor.redact(body, format, phase);
        if (maskedBody == body) { //NOSONAR S1698: intentional reference equality — fast-path when body is unchanged
            return attrs;
        }
        AttributesBuilder builder = Attributes.builder();
        attrMap.forEach((key, value) -> {
            if (PAYLOAD_BODY.equals(key)) {
                builder.put(PAYLOAD_BODY, maskedBody);
            } else {
                putRaw(builder, key, value);
            }
        });
        return builder.build();
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
