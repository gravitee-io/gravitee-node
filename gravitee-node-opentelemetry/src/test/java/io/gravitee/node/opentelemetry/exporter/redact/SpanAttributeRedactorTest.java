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
import io.gravitee.node.api.opentelemetry.redaction.PartialMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import io.gravitee.node.api.opentelemetry.redaction.RedactionRule;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SpanAttributeRedactorTest {

    @Test
    void should_return_same_attributes_instance_when_no_rules_configured() {
        var redactor = new SpanAttributeRedactor(RedactionConfig.EMPTY);
        var attrs = Attributes.of(AttributeKey.stringKey("foo"), "bar");

        assertThat(redactor.redact(attrs)).isSameAs(attrs);
    }

    @Test
    void should_return_same_attributes_instance_when_no_key_matches() {
        var redactor = redactor("db.statement");
        var attrs = Attributes.of(AttributeKey.stringKey("http.method"), "GET");

        assertThat(redactor.redact(attrs)).isSameAs(attrs);
    }

    @Test
    void should_redact_exact_matching_string_attribute() {
        var redactor = redactor("enduser.id");
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_not_redact_non_matching_attribute() {
        var redactor = redactor("enduser.id");
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice", AttributeKey.stringKey("http.method"), "GET");

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("http.method"))).isEqualTo("GET");
    }

    @Test
    void should_redact_attributes_matching_single_star_glob() {
        var redactor = redactor("http.request.header.*");
        var attrs = Attributes.of(
            AttributeKey.stringKey("http.request.header.authorization"),
            "Bearer secret",
            AttributeKey.stringKey("http.request.header.x-api-key"),
            "key123",
            AttributeKey.stringKey("http.method"),
            "GET"
        );

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("http.request.header.authorization"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("http.request.header.x-api-key"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("http.method"))).isEqualTo("GET");
    }

    @Test
    void should_not_cross_dot_boundary_with_single_star() {
        var redactor = redactor("http.request.*");
        var attrs = Attributes.of(
            AttributeKey.stringKey("http.request.header.authorization"),
            "Bearer secret",
            AttributeKey.stringKey("http.request.method"),
            "GET"
        );

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("http.request.header.authorization"))).isEqualTo("Bearer secret");
        assertThat(result.get(AttributeKey.stringKey("http.request.method"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void double_star_alone_matches_all_attribute_keys() {
        // "**" is the correct pattern to scan every attribute regardless of name
        var rule = new RedactionRule("**", MaskingStrategy.DEFAULT, "5[1-5][0-9]{14}");
        var redactor = new SpanAttributeRedactor(new RedactionConfig(java.util.List.of(rule)));
        var attrs = Attributes.of(
            AttributeKey.stringKey("payment.card.number"),
            "5111111111111111",
            AttributeKey.stringKey("http.method"),
            "GET"
        );

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("payment.card.number"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("http.method"))).isEqualTo("GET");
    }

    @Test
    void single_star_alone_does_not_match_dotted_attribute_keys() {
        // "*" only matches single-segment keys (no dots) — use "**" to match all
        var redactor = redactor("*");
        var attrs = Attributes.of(AttributeKey.stringKey("http.method"), "GET");

        assertThat(redactor.redact(attrs)).isSameAs(attrs);
    }

    @Test
    void should_cross_dot_boundary_with_double_star_glob() {
        var redactor = redactor("http.request.**");
        var attrs = Attributes.of(
            AttributeKey.stringKey("http.request.header.authorization"),
            "Bearer secret",
            AttributeKey.stringKey("http.request.method"),
            "GET",
            AttributeKey.stringKey("http.response.status"),
            "200"
        );

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("http.request.header.authorization"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("http.request.method"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("http.response.status"))).isEqualTo("200");
    }

    @Test
    void should_redact_attributes_matching_regex_pattern() {
        var redactor = redactor("regex:enduser\\.(id|email)");
        var attrs = Attributes.of(
            AttributeKey.stringKey("enduser.id"),
            "alice",
            AttributeKey.stringKey("enduser.email"),
            "alice@example.com",
            AttributeKey.stringKey("enduser.role"),
            "admin"
        );

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("enduser.email"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.stringKey("enduser.role"))).isEqualTo("admin");
    }

    @Test
    void should_use_custom_replacement_when_configured() {
        var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(new RedactionRule("enduser.id", "***"))));
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("***");
    }

    @Test
    void should_redact_long_attribute_as_string() {
        var redactor = redactor("user.id");
        var attrs = Attributes.of(AttributeKey.longKey("user.id"), 12345L);

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("user.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(result.get(AttributeKey.longKey("user.id"))).isNull();
    }

    @Test
    void should_preserve_non_matching_long_attribute() {
        var redactor = redactor("enduser.id");
        var attrs = Attributes.of(AttributeKey.longKey("http.status_code"), 200L);

        assertThat(redactor.redact(attrs)).isSameAs(attrs);
    }

    @Test
    void should_handle_null_attributes_gracefully() {
        assertThat(redactor("anything").redact(null)).isNull();
    }

    @Test
    void should_return_empty_attributes_unchanged() {
        assertThat(redactor("anything").redact(Attributes.empty())).isSameAs(Attributes.empty());
    }

    @Test
    void should_apply_first_matching_rule_replacement() {
        var rules = List.of(new RedactionRule("enduser.*", "RULE_1"), new RedactionRule("enduser.id", "RULE_2"));
        var redactor = new SpanAttributeRedactor(new RedactionConfig(rules));
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("RULE_1");
    }

    @Test
    void should_report_no_rules_for_empty_config() {
        assertThat(new SpanAttributeRedactor(RedactionConfig.EMPTY).hasRules()).isFalse();
    }

    @Test
    void should_report_rules_when_configured() {
        assertThat(redactor("anything").hasRules()).isTrue();
    }

    @Test
    void should_redact_only_when_value_matches_value_pattern() {
        var rule = new RedactionRule("enduser.id", MaskingStrategy.DEFAULT, "^alice$");
        var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_not_redact_when_value_does_not_match_value_pattern() {
        var rule = new RedactionRule("enduser.id", MaskingStrategy.DEFAULT, "^alice$");
        var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "bob");

        assertThat(redactor.redact(attrs)).isSameAs(attrs);
    }

    @Test
    void should_redact_all_matching_attributes_regardless_of_other_span_attributes() {
        var rules = List.of(new RedactionRule("req.secret", "[R1]"), new RedactionRule("res.secret", "[R2]"));
        var redactor = new SpanAttributeRedactor(new RedactionConfig(rules));
        var attrs = Attributes.of(
            AttributeKey.stringKey("req.secret"),
            "sensitive",
            AttributeKey.stringKey("res.secret"),
            "also-sensitive"
        );

        var result = redactor.redact(attrs);

        assertThat(result.get(AttributeKey.stringKey("req.secret"))).isEqualTo("[R1]");
        assertThat(result.get(AttributeKey.stringKey("res.secret"))).isEqualTo("[R2]");
    }

    @Test
    void should_apply_custom_full_replacement_string() {
        var redactor = new SpanAttributeRedactor(
            new RedactionConfig(List.of(new RedactionRule("enduser.id", MaskingStrategy.fullMask("<<<<>>>"))))
        );
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("<<<<>>>");
    }

    @Test
    void should_apply_partial_mask_hiding_middle_keeping_suffix() {
        var redactor = new SpanAttributeRedactor(
            new RedactionConfig(List.of(new RedactionRule("payment.card", MaskingStrategy.partialMask(0, 4))))
        );
        var attrs = Attributes.of(AttributeKey.stringKey("payment.card"), "4111111111111111");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("payment.card"))).isEqualTo("************1111");
    }

    @Test
    void should_apply_partial_mask_keeping_prefix() {
        var redactor = new SpanAttributeRedactor(
            new RedactionConfig(List.of(new RedactionRule("http.request.header.authorization", MaskingStrategy.partialMask(7, 0))))
        );
        var attrs = Attributes.of(AttributeKey.stringKey("http.request.header.authorization"), "Bearer eyJhbGciOiJSUzI1NiJ9");

        var result = redactor.redact(attrs).get(AttributeKey.stringKey("http.request.header.authorization"));
        assertThat(result).startsWith("Bearer ");
        assertThat(result).doesNotContain("eyJhbGciOiJSUzI1NiJ9");
        assertThat(result).hasSize("Bearer eyJhbGciOiJSUzI1NiJ9".length());
    }

    @Test
    void should_apply_partial_mask_keeping_prefix_and_suffix() {
        var redactor = new SpanAttributeRedactor(
            new RedactionConfig(List.of(new RedactionRule("api.key", MaskingStrategy.partialMask(3, 3))))
        );
        var attrs = Attributes.of(AttributeKey.stringKey("api.key"), "sk-live-abc123xyz");

        var result = redactor.redact(attrs).get(AttributeKey.stringKey("api.key"));
        assertThat(result).startsWith("sk-");
        assertThat(result).endsWith("xyz");
        assertThat(result).contains("*");
    }

    @Test
    void should_apply_custom_mask_character_for_partial_mask() {
        var redactor = new SpanAttributeRedactor(
            new RedactionConfig(List.of(new RedactionRule("enduser.id", MaskingStrategy.partialMask(2, 2, "X"))))
        );
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice123");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("alXXXX23");
    }

    @Test
    void should_mask_entire_value_when_too_short_for_prefix_and_suffix() {
        var redactor = new SpanAttributeRedactor(
            new RedactionConfig(List.of(new RedactionRule("enduser.id", MaskingStrategy.partialMask(3, 3))))
        );
        var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "ab");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("**");
    }

    @Test
    void should_preserve_original_value_length_with_partial_mask() {
        var value = "4111 1111 1111 1111";
        var redactor = new SpanAttributeRedactor(
            new RedactionConfig(List.of(new RedactionRule("payment.card", MaskingStrategy.partialMask(0, 4))))
        );
        var attrs = Attributes.of(AttributeKey.stringKey("payment.card"), value);

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("payment.card"))).hasSameSizeAs(value);
    }

    @Test
    void should_throw_when_partial_mask_char_is_multi_character() {
        assertThatThrownBy(() -> new PartialMaskingStrategy(0, 4, "XX"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactly one character");
    }

    @Test
    void should_throw_when_partial_mask_factory_receives_multi_char_mask_char() {
        assertThatThrownBy(() -> MaskingStrategy.partialMask(0, 4, "**"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactly one character");
    }

    @Test
    void should_accept_single_char_mask_character() {
        var strategy = (PartialMaskingStrategy) MaskingStrategy.partialMask(0, 4, "X");
        assertThat(strategy.maskChar()).isEqualTo("X");
    }

    @Test
    void should_redact_dot_separated_attribute_using_short_name() {
        var redactor = redactor("user-id");
        var attrs = Attributes.of(AttributeKey.stringKey("gravitee.attribute.user-id"), "alice");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("gravitee.attribute.user-id")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_redact_bare_attribute_using_short_name() {
        var redactor = redactor("custom");
        var attrs = Attributes.of(AttributeKey.stringKey("custom"), "secret-value");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("custom"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_redact_underscore_separated_attribute_using_short_name() {
        var redactor = redactor("my_id");
        var attrs = Attributes.of(AttributeKey.stringKey("http.request.my_id"), "value");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("http.request.my_id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_redact_old_semconv_underscore_attribute_using_short_name() {
        var redactor = redactor("content_length");
        var attrs = Attributes.of(AttributeKey.stringKey("http.response_content_length"), "1024");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("http.response_content_length")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_redact_status_code_using_short_name_regardless_of_separator() {
        var redactor = redactor("status_code");
        var dotAttrs = Attributes.of(AttributeKey.stringKey("http.response.status_code"), "200");
        var underscoreAttrs = Attributes.of(AttributeKey.stringKey("http.status_code"), "200");

        assertThat(redactor.redact(dotAttrs).get(AttributeKey.stringKey("http.response.status_code")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        assertThat(redactor.redact(underscoreAttrs).get(AttributeKey.stringKey("http.status_code")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_not_partially_match_hyphenated_short_name() {
        // api-key should NOT match x-api-key (different attribute)
        var redactor = redactor("api-key");
        var attrs = Attributes.of(AttributeKey.stringKey("http.request.header.x-api-key"), "secret");

        assertThat(redactor.redact(attrs)).isSameAs(attrs);
    }

    @Test
    void should_not_apply_short_name_expansion_when_full_pattern_given() {
        // full name with dots is NOT expanded — exact glob matching applies
        var redactor = redactor("gravitee.attribute.user-id");
        var attrs = Attributes.of(AttributeKey.stringKey("gravitee.attribute.user-id"), "alice");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("gravitee.attribute.user-id")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_redact_when_attribute_key_casing_differs_from_pattern() {
        var redactor = redactor("http.request.header.authorization");
        var attrs = Attributes.of(AttributeKey.stringKey("http.request.header.Authorization"), "Bearer secret");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("http.request.header.Authorization")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_redact_when_pattern_is_uppercase_and_attribute_is_lowercase() {
        var redactor = redactor("HTTP.REQUEST.HEADER.AUTHORIZATION");
        var attrs = Attributes.of(AttributeKey.stringKey("http.request.header.authorization"), "Bearer secret");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("http.request.header.authorization")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    @Test
    void should_redact_glob_case_insensitive() {
        var redactor = redactor("http.request.header.**");
        var attrs = Attributes.of(AttributeKey.stringKey("http.request.header.X-Api-Key"), "my-key");

        assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("http.request.header.X-Api-Key")))
            .isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
    }

    // -------------------------------------------------------------------------
    // Contract: config-level defaultReplacement
    // -------------------------------------------------------------------------

    @Nested
    class ConfigDefaultReplacement {

        @Test
        void uses_config_default_replacement_when_rule_has_no_explicit_strategy() {
            // Rule with no explicit replacement uses MaskingStrategy.DEFAULT → resolved from config.
            var config = new RedactionConfig(List.of(new RedactionRule("enduser.id")), "[HIDDEN]");
            var redactor = new SpanAttributeRedactor(config);
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("[HIDDEN]");
        }

        @Test
        void explicit_rule_replacement_is_not_overridden_by_config_default() {
            // fullMask("MY_MASK") is an explicit strategy — config default must not override it.
            var config = new RedactionConfig(List.of(new RedactionRule("enduser.id", MaskingStrategy.fullMask("MY_MASK"))), "[HIDDEN]");
            var redactor = new SpanAttributeRedactor(config);
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("MY_MASK");
        }

        @Test
        void explicit_full_mask_with_same_string_as_default_is_not_overridden_by_config_default() {
            // fullMask("[REDACTED]") has the same field values as MaskingStrategy.DEFAULT but is a
            // NEW instance — it represents an explicit choice and must NOT be overridden by config.
            // This test catches a regression where .equals() was used instead of == for the sentinel.
            var config = new RedactionConfig(List.of(new RedactionRule("enduser.id", MaskingStrategy.fullMask("[REDACTED]"))), "[HIDDEN]");
            var redactor = new SpanAttributeRedactor(config);
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

            // Must use the rule's explicit "[REDACTED]", NOT the config default "[HIDDEN]"
            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo("[REDACTED]");
        }

        @Test
        void partial_mask_rule_is_not_affected_by_config_default_replacement() {
            var config = new RedactionConfig(List.of(new RedactionRule("payment.card", MaskingStrategy.partialMask(0, 4))), "[HIDDEN]");
            var redactor = new SpanAttributeRedactor(config);
            var attrs = Attributes.of(AttributeKey.stringKey("payment.card"), "4111111111111111");

            // Partial mask outcome depends on the mask char, not the config default replacement.
            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("payment.card"))).isEqualTo("************1111");
        }

        @Test
        void null_config_default_falls_back_to_built_in_default() {
            // null is normalised to "[REDACTED]" in the RedactionConfig compact constructor.
            var config = new RedactionConfig(List.of(new RedactionRule("enduser.id")), null);
            var redactor = new SpanAttributeRedactor(config);
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }

        @Test
        void blank_config_default_falls_back_to_built_in_default() {
            var config = new RedactionConfig(List.of(new RedactionRule("enduser.id")), "   ");
            var redactor = new SpanAttributeRedactor(config);
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }

        @Test
        void single_arg_constructor_uses_built_in_default() {
            // Verify backward-compat constructor still produces "[REDACTED]".
            var config = new RedactionConfig(List.of(new RedactionRule("enduser.id")));
            var redactor = new SpanAttributeRedactor(config);
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }
    }

    // -------------------------------------------------------------------------
    // Contract: value-pattern semantics (find vs matches, case sensitivity)
    // -------------------------------------------------------------------------

    @Nested
    class ValuePatternSemantics {

        @Test
        void value_pattern_is_substring_match_not_full_string() {
            // valuePattern uses find() — "Bearer" matches "Bearer eyJhbGci…" without anchoring.
            var rule = new RedactionRule("enduser.token", MaskingStrategy.DEFAULT, "Bearer");
            var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.token"), "Bearer eyJhbGciOiJSUzI1NiJ9.some.payload");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.token"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }

        @Test
        void value_pattern_matches_when_value_contains_pattern_as_substring() {
            // "alice" appears somewhere inside the value — find() fires.
            var rule = new RedactionRule("enduser.id", MaskingStrategy.DEFAULT, "alice");
            var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "prefix-alice-suffix");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }

        @Test
        void value_pattern_does_not_match_when_anchored_and_value_has_extra_content() {
            // Operators must use ^…$ anchors when they want a full-string match.
            var rule = new RedactionRule("enduser.id", MaskingStrategy.DEFAULT, "^alice$");
            var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "prefix-alice");

            assertThat(redactor.redact(attrs)).isSameAs(attrs);
        }

        @Test
        void value_pattern_is_case_sensitive_unlike_key_pattern() {
            // Key patterns are CASE_INSENSITIVE; value patterns are NOT.
            var rule = new RedactionRule("enduser.id", MaskingStrategy.DEFAULT, "Alice");
            var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "alice"); // lowercase — no match

            assertThat(redactor.redact(attrs)).isSameAs(attrs);
        }

        @Test
        void value_pattern_case_sensitive_matches_on_exact_case() {
            var rule = new RedactionRule("enduser.id", MaskingStrategy.DEFAULT, "Alice");
            var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
            var attrs = Attributes.of(AttributeKey.stringKey("enduser.id"), "Alice");

            assertThat(redactor.redact(attrs).get(AttributeKey.stringKey("enduser.id"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
        }
    }

    // -------------------------------------------------------------------------
    // Contract: type coercion when non-string attributes are redacted
    // -------------------------------------------------------------------------

    @Nested
    class TypeCoercion {

        @Test
        void redacted_boolean_attribute_is_written_back_as_string() {
            // Boolean type is coerced to String when redacted; original typed key is removed.
            var redactor = redactor("feature.enabled");
            var attrs = Attributes.of(AttributeKey.booleanKey("feature.enabled"), true);

            var result = redactor.redact(attrs);

            assertThat(result.get(AttributeKey.stringKey("feature.enabled"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
            assertThat(result.get(AttributeKey.booleanKey("feature.enabled"))).isNull();
        }

        @Test
        void redacted_double_attribute_is_written_back_as_string() {
            var redactor = redactor("billing.amount");
            var attrs = Attributes.of(AttributeKey.doubleKey("billing.amount"), 99.99);

            var result = redactor.redact(attrs);

            assertThat(result.get(AttributeKey.stringKey("billing.amount"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
            assertThat(result.get(AttributeKey.doubleKey("billing.amount"))).isNull();
        }

        @Test
        void redacted_string_array_attribute_is_written_back_as_string() {
            var redactor = redactor("user.roles");
            var attrs = Attributes.of(AttributeKey.stringArrayKey("user.roles"), List.of("admin", "user"));

            var result = redactor.redact(attrs);

            assertThat(result.get(AttributeKey.stringKey("user.roles"))).isEqualTo(RedactionRule.DEFAULT_REPLACEMENT);
            assertThat(result.get(AttributeKey.stringArrayKey("user.roles"))).isNull();
        }

        @Test
        void non_matching_typed_attributes_preserve_their_original_type() {
            var redactor = redactor("enduser.id");
            var attrs = Attributes.of(
                AttributeKey.longKey("http.status_code"),
                200L,
                AttributeKey.booleanKey("http.request.resend_count"),
                false
            );

            assertThat(redactor.redact(attrs)).isSameAs(attrs);
        }
    }

    // -------------------------------------------------------------------------
    // Contract: MaskingStrategy fail-fast on invalid construction arguments
    // -------------------------------------------------------------------------

    @Nested
    class MaskingStrategyValidation {

        @Test
        void should_throw_on_negative_prefix_length() {
            assertThatThrownBy(() -> MaskingStrategy.partialMask(-1, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prefixLength");
        }

        @Test
        void should_throw_on_negative_suffix_length() {
            assertThatThrownBy(() -> MaskingStrategy.partialMask(4, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("suffixLength");
        }

        @Test
        void should_accept_zero_prefix_and_suffix() {
            // zero is a valid boundary value — must not throw
            var strategy = (PartialMaskingStrategy) MaskingStrategy.partialMask(0, 0);
            assertThat(strategy.prefixLength()).isZero();
            assertThat(strategy.suffixLength()).isZero();
        }
    }

    // -------------------------------------------------------------------------
    // Contract: fail-fast on invalid patterns at construction time
    // -------------------------------------------------------------------------

    @Nested
    class PatternValidation {

        @Test
        void should_throw_on_invalid_regex_key_pattern_at_construction() {
            var rule = new RedactionRule("regex:[unclosed-bracket");
            assertThatThrownBy(() -> new SpanAttributeRedactor(new RedactionConfig(List.of(rule))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[unclosed-bracket");
        }

        @Test
        void should_throw_on_invalid_regex_value_pattern_at_construction() {
            var rule = new RedactionRule("enduser.id", MaskingStrategy.DEFAULT, "[bad-value-regex");
            assertThatThrownBy(() -> new SpanAttributeRedactor(new RedactionConfig(List.of(rule))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[bad-value-regex");
        }

        @Test
        void should_accept_valid_regex_key_pattern() {
            var rule = new RedactionRule("regex:enduser\\.(id|email)");
            // must not throw
            var redactor = new SpanAttributeRedactor(new RedactionConfig(List.of(rule)));
            assertThat(redactor.hasRules()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static SpanAttributeRedactor redactor(String pattern) {
        return new SpanAttributeRedactor(new RedactionConfig(List.of(new RedactionRule(pattern))));
    }
}
