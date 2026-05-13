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

import com.jayway.jsonpath.JsonPath;
import io.gravitee.node.api.opentelemetry.redaction.FullMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PartialMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PayloadFormat;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingRule;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;

/**
 * A pre-compiled form of a {@link PayloadMaskingRule} used at export time.
 * Path expressions are compiled once at deploy time — not per request — to avoid
 * repeated parse overhead on the hot path.
 */
final class CompiledPayloadMaskingRule {

    // null when format is XML, or when JsonPath.compile() fails for AUTO/JSON rules
    private final JsonPath compiledJsonPath;
    // raw XPath expression; always set for XML and AUTO formats
    private final String rawXPath;
    private final MaskingStrategy maskingStrategy;
    private final String effectiveReplacement;
    private final PayloadPhase phase;
    private final PayloadFormat format;

    CompiledPayloadMaskingRule(PayloadMaskingRule rule, String configDefaultReplacement) {
        this.maskingStrategy = rule.maskingStrategy();
        this.phase = rule.phase();
        this.format = rule.format();
        this.effectiveReplacement =
            switch (rule.maskingStrategy()) {
                case FullMaskingStrategy full -> (full == MaskingStrategy.DEFAULT) ? configDefaultReplacement : full.replacement();
                case PartialMaskingStrategy ignored -> null; // computed per value
            };

        if (rule.format() == PayloadFormat.XML) {
            this.compiledJsonPath = null;
            this.rawXPath = rule.path();
        } else {
            // JSON or AUTO: compile as JsonPath. AUTO rules also run through XML redactor if body is XML.
            JsonPath compiled;
            try {
                compiled = JsonPath.compile(rule.path());
            } catch (Exception e) {
                compiled = null; // invalid JsonPath expression — rule will be skipped at redact time
            }
            this.compiledJsonPath = compiled;
            // Keep the raw path for XPath evaluation when AUTO detects XML at runtime.
            this.rawXPath = rule.path();
        }
    }

    JsonPath compiledJsonPath() {
        return compiledJsonPath;
    }

    String rawXPath() {
        return rawXPath;
    }

    PayloadFormat format() {
        return format;
    }

    boolean appliesToPhase(PayloadPhase requestedPhase) {
        return this.phase == PayloadPhase.BOTH || this.phase == requestedPhase;
    }

    String applyMask(String value) {
        return switch (maskingStrategy) {
            case FullMaskingStrategy ignored -> effectiveReplacement;
            case PartialMaskingStrategy partial -> {
                String maskChar = partial.maskChar();
                if (value == null) yield maskChar;
                int prefix = partial.prefixLength();
                int suffix = partial.suffixLength();
                if (value.length() <= prefix + suffix) yield maskChar.repeat(value.length());
                yield value.substring(0, prefix) +
                maskChar.repeat(value.length() - prefix - suffix) +
                value.substring(value.length() - suffix);
            }
        };
    }
}
