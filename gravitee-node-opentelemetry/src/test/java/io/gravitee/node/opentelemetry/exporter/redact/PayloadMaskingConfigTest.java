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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PayloadFormat;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingConfig;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingRule;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import io.gravitee.node.api.opentelemetry.redaction.RedactionRule;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PayloadMaskingConfigTest {

    // --- PayloadMaskingRule ---

    @Test
    void should_require_non_null_path() {
        assertThatThrownBy(() -> new PayloadMaskingRule(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("path");
    }

    @Test
    void should_default_strategy_to_DEFAULT() {
        var rule = new PayloadMaskingRule("$.password");
        assertThat(rule.maskingStrategy()).isEqualTo(MaskingStrategy.DEFAULT);
    }

    @Test
    void should_default_phase_to_BOTH() {
        var rule = new PayloadMaskingRule("$.password");
        assertThat(rule.phase()).isEqualTo(PayloadPhase.BOTH);
    }

    @Test
    void should_default_format_to_AUTO() {
        var rule = new PayloadMaskingRule("$.password");
        assertThat(rule.format()).isEqualTo(PayloadFormat.AUTO);
    }

    @Test
    void should_coerce_null_strategy_to_DEFAULT() {
        var rule = new PayloadMaskingRule("$.password", null, PayloadPhase.REQUEST, PayloadFormat.JSON);
        assertThat(rule.maskingStrategy()).isEqualTo(MaskingStrategy.DEFAULT);
    }

    @Test
    void should_coerce_null_phase_to_BOTH() {
        var rule = new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, null, PayloadFormat.JSON);
        assertThat(rule.phase()).isEqualTo(PayloadPhase.BOTH);
    }

    @Test
    void should_coerce_null_format_to_AUTO() {
        var rule = new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.REQUEST, null);
        assertThat(rule.format()).isEqualTo(PayloadFormat.AUTO);
    }

    @Test
    void should_preserve_explicit_values() {
        var strategy = MaskingStrategy.partialMask(2, 2);
        var rule = new PayloadMaskingRule("//cvv", strategy, PayloadPhase.RESPONSE, PayloadFormat.XML);
        assertThat(rule.path()).isEqualTo("//cvv");
        assertThat(rule.maskingStrategy()).isEqualTo(strategy);
        assertThat(rule.phase()).isEqualTo(PayloadPhase.RESPONSE);
        assertThat(rule.format()).isEqualTo(PayloadFormat.XML);
    }

    // --- PayloadMaskingConfig ---

    @Test
    void EMPTY_should_have_no_rules() {
        assertThat(PayloadMaskingConfig.EMPTY.hasRules()).isFalse();
        assertThat(PayloadMaskingConfig.EMPTY.rules()).isEmpty();
    }

    @Test
    void EMPTY_should_use_default_replacement() {
        assertThat(PayloadMaskingConfig.EMPTY.defaultReplacement()).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_coerce_null_rules_to_empty_list() {
        var config = new PayloadMaskingConfig(null);
        assertThat(config.hasRules()).isFalse();
    }

    @Test
    void should_coerce_null_defaultReplacement_to_default() {
        var config = new PayloadMaskingConfig(List.of(), null);
        assertThat(config.defaultReplacement()).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_coerce_blank_defaultReplacement_to_default() {
        var config = new PayloadMaskingConfig(List.of(), "   ");
        assertThat(config.defaultReplacement()).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void rules_list_should_be_immutable() {
        var rule = new PayloadMaskingRule("$.password");
        var config = new PayloadMaskingConfig(List.of(rule));
        assertThatThrownBy(() -> config.rules().add(new PayloadMaskingRule("$.other"))).isInstanceOf(UnsupportedOperationException.class);
    }

    // --- mergeWith ---

    @Test
    void mergeWith_should_return_this_when_other_is_null() {
        var ruleA = new PayloadMaskingRule("$.password");
        var config = new PayloadMaskingConfig(List.of(ruleA));
        assertThat(config.mergeWith(null)).isSameAs(config);
    }

    @Test
    void mergeWith_should_return_this_when_other_has_no_rules() {
        var ruleA = new PayloadMaskingRule("$.password");
        var config = new PayloadMaskingConfig(List.of(ruleA));
        assertThat(config.mergeWith(PayloadMaskingConfig.EMPTY)).isSameAs(config);
    }

    @Test
    void mergeWith_should_return_other_when_this_has_no_rules() {
        var ruleB = new PayloadMaskingRule("$.creditCard");
        var other = new PayloadMaskingConfig(List.of(ruleB));
        assertThat(PayloadMaskingConfig.EMPTY.mergeWith(other)).isSameAs(other);
    }

    @Test
    void mergeWith_should_put_global_rules_first_then_api_rules() {
        var ruleA = new PayloadMaskingRule("$.password");
        var ruleB = new PayloadMaskingRule("$.creditCard");
        var ruleC = new PayloadMaskingRule("//cvv", MaskingStrategy.DEFAULT, PayloadPhase.REQUEST, PayloadFormat.XML);
        var global = new PayloadMaskingConfig(List.of(ruleA, ruleB));
        var perApi = new PayloadMaskingConfig(List.of(ruleC));

        var merged = global.mergeWith(perApi);

        assertThat(merged.rules()).containsExactly(ruleA, ruleB, ruleC);
    }

    @Test
    void mergeWith_should_keep_defaultReplacement_from_this() {
        var ruleA = new PayloadMaskingRule("$.password");
        var ruleB = new PayloadMaskingRule("$.creditCard");
        var global = new PayloadMaskingConfig(List.of(ruleA), "***");
        var perApi = new PayloadMaskingConfig(List.of(ruleB), "XXX");

        var merged = global.mergeWith(perApi);

        assertThat(merged.defaultReplacement()).isEqualTo("***");
    }

    @Test
    void mergeWith_merged_rules_list_should_be_immutable() {
        var ruleA = new PayloadMaskingRule("$.password");
        var ruleB = new PayloadMaskingRule("$.creditCard");
        var ruleC = new PayloadMaskingRule("//cvv", MaskingStrategy.DEFAULT, PayloadPhase.REQUEST, PayloadFormat.XML);
        var global = new PayloadMaskingConfig(List.of(ruleA));
        var perApi = new PayloadMaskingConfig(List.of(ruleB));

        var merged = global.mergeWith(perApi);

        assertThatThrownBy(() -> merged.rules().add(ruleC)).isInstanceOf(UnsupportedOperationException.class);
    }
}
