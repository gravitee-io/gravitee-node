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

public record RedactionConfig(List<RedactionRule> rules, String defaultReplacement) {
    public static final RedactionConfig EMPTY = new RedactionConfig(List.of());

    public RedactionConfig {
        rules = (rules == null) ? List.of() : List.copyOf(rules);
        defaultReplacement =
            (defaultReplacement == null || defaultReplacement.isBlank()) ? RedactionRule.DEFAULT_REPLACEMENT : defaultReplacement;
    }

    public RedactionConfig(List<RedactionRule> rules) {
        this(rules, RedactionRule.DEFAULT_REPLACEMENT);
    }

    public boolean hasRules() {
        return !rules.isEmpty();
    }

    public RedactionConfig mergeWith(RedactionConfig config) {
        if (config == null || !config.hasRules()) return this;
        if (!this.hasRules()) return config;
        List<RedactionRule> combined = new ArrayList<>(this.rules());
        combined.addAll(config.rules());
        return new RedactionConfig(combined, this.defaultReplacement());
    }
}
