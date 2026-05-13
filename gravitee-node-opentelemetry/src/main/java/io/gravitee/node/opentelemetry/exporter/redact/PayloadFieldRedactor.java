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

import io.gravitee.node.api.opentelemetry.redaction.PayloadFormat;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingConfig;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import java.util.List;

/**
 * Dispatcher that routes a payload body to {@link JsonPathPayloadRedactor} or
 * {@link XPathPayloadRedactor} based on the resolved {@link PayloadFormat}.
 *
 * <p>Format resolution order for rules whose format is {@link PayloadFormat#AUTO}:
 * <ol>
 *   <li>The {@code eventFormat} attribute from the span event ({@code "JSON"} / {@code "XML"}).</li>
 *   <li>Body heuristic: stripped body starting with {@code {} or {@code [} → JSON;
 *       starting with {@code <} → XML.</li>
 * </ol>
 *
 * <p>Thread-safe. Holds no mutable state after construction.
 */
final class PayloadFieldRedactor {

    private final JsonPathPayloadRedactor jsonRedactor = new JsonPathPayloadRedactor();
    private final XPathPayloadRedactor xmlRedactor = new XPathPayloadRedactor();

    private final List<CompiledPayloadMaskingRule> jsonRules;
    private final List<CompiledPayloadMaskingRule> xmlRules;
    private final List<CompiledPayloadMaskingRule> autoRules;

    PayloadFieldRedactor(PayloadMaskingConfig config) {
        String defaultReplacement = config.defaultReplacement();
        List<CompiledPayloadMaskingRule> compiled = config
            .rules()
            .stream()
            .map(rule -> new CompiledPayloadMaskingRule(rule, defaultReplacement))
            .toList();

        this.jsonRules = compiled.stream().filter(r -> r.format == PayloadFormat.JSON).toList();
        this.xmlRules = compiled.stream().filter(r -> r.format == PayloadFormat.XML).toList();
        this.autoRules = compiled.stream().filter(r -> r.format == PayloadFormat.AUTO).toList();
    }

    /**
     * Masks sensitive fields in {@code body} using rules that apply to {@code phase}.
     *
     * @param body        the raw payload body string from the span event's {@code payload.body} attribute
     * @param eventFormat the {@code payload.format} span-event attribute value (may be null → triggers AUTO heuristic)
     * @param phase       {@code REQUEST} or {@code RESPONSE} from the {@code payload.phase} attribute
     * @return the masked body, or {@code body} unchanged if no rules match
     */
    String redact(String body, String eventFormat, PayloadPhase phase) {
        if (body == null || body.isBlank()) {
            return body;
        }

        PayloadFormat resolved = resolveFormat(eventFormat, body);

        return switch (resolved) {
            case JSON -> {
                String result = jsonRedactor.redact(body, jsonRules, phase);
                // Also apply AUTO rules as JSON
                yield autoRules.isEmpty() ? result : jsonRedactor.redact(result, autoRules, phase);
            }
            case XML -> {
                String result = xmlRedactor.redact(body, xmlRules, phase);
                // Also apply AUTO rules as XML
                yield autoRules.isEmpty() ? result : xmlRedactor.redact(result, autoRules, phase);
            }
            case AUTO -> body; // unrecognised format — fail-open, do not mask
        };
    }

    private static PayloadFormat resolveFormat(String eventFormat, String body) {
        if (eventFormat != null) {
            if ("JSON".equalsIgnoreCase(eventFormat)) return PayloadFormat.JSON;
            if ("XML".equalsIgnoreCase(eventFormat)) return PayloadFormat.XML;
        }
        // Body heuristic
        String stripped = body.stripLeading();
        if (!stripped.isEmpty()) {
            char first = stripped.charAt(0);
            if (first == '{' || first == '[') return PayloadFormat.JSON;
            if (first == '<') return PayloadFormat.XML;
        }
        return PayloadFormat.AUTO; // unrecognised
    }
}
