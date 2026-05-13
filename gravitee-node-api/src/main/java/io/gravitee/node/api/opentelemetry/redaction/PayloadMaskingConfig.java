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

import java.util.ArrayList;
import java.util.List;

/**
 * An ordered, immutable collection of {@link PayloadMaskingRule} entries used to mask sensitive
 * fields in request/response payload bodies before export via OTLP.
 *
 * <p>Mirrors {@link RedactionConfig} in structure so both configs can be active simultaneously
 * without interference. Global (gravitee.yml) rules are the base; per-API rules are appended via
 * {@link #mergeWith(PayloadMaskingConfig)}.
 *
 * @param rules              ordered list of masking rules; never null, never contains nulls
 * @param defaultReplacement fallback replacement string used by rules whose strategy is
 *                           {@link MaskingStrategy#DEFAULT}
 */
public record PayloadMaskingConfig(List<PayloadMaskingRule> rules, String defaultReplacement) {
    public static final PayloadMaskingConfig EMPTY = new PayloadMaskingConfig(List.of());

    public PayloadMaskingConfig {
        rules = (rules == null) ? List.of() : List.copyOf(rules);
        defaultReplacement =
            (defaultReplacement == null || defaultReplacement.isBlank()) ? RedactionRule.DEFAULT_REPLACEMENT : defaultReplacement;
    }

    public PayloadMaskingConfig(List<PayloadMaskingRule> rules) {
        this(rules, RedactionRule.DEFAULT_REPLACEMENT);
    }

    public boolean hasRules() {
        return !rules.isEmpty();
    }

    /**
     * Returns a new config whose rules are this config's rules followed by {@code other}'s rules.
     * Global (YAML) config should call this with the per-API config as argument so global rules
     * are evaluated first.
     */
    public PayloadMaskingConfig mergeWith(PayloadMaskingConfig other) {
        if (other == null || !other.hasRules()) return this;
        if (!this.hasRules()) return other;
        List<PayloadMaskingRule> combined = new ArrayList<>(this.rules());
        combined.addAll(other.rules());
        return new PayloadMaskingConfig(combined, this.defaultReplacement());
    }
}
