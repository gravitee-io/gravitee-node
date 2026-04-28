package io.gravitee.node.opentelemetry.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.opentelemetry.redaction.FullMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PartialMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import io.gravitee.node.api.opentelemetry.redaction.RedactionRule;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.internal.AttributesMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenTelemetryConfigurationTest {

    MockEnvironment environment = new MockEnvironment();
    OpenTelemetryConfiguration underTest = new OpenTelemetryConfiguration(environment);

    @Test
    void should_get_custom_headers() {
        environment.setProperty("services.opentelemetry.exporter.headers[0].foo", "bar");
        environment.setProperty("services.opentelemetry.exporter.headers[1].baz", "puk");
        Map<String, String> propertyMap = underTest.getCustomHeaders();
        assertThat(propertyMap).containsAllEntriesOf(Map.of("foo", "bar", "baz", "puk"));
    }

    @Test
    void should_get_custom_headers_from_fallback() {
        environment.setProperty("services.tracing.otel.headers[0].foo", "bar");
        environment.setProperty("services.tracing.otel.headers[1].baz", "puk");
        Map<String, String> propertyMap = underTest.getCustomHeaders();
        assertThat(propertyMap).containsAllEntriesOf(Map.of("foo", "bar", "baz", "puk"));
    }

    @Test
    void should_get_empty_custom_headers() {
        Map<String, String> propertyMap = underTest.getCustomHeaders();
        assertThat(propertyMap).isEmpty();
    }

    @Test
    void should_get_extra_attributes() {
        environment.setProperty("services.opentelemetry.extraAttributes[0].foo", "bar");
        environment.setProperty("services.opentelemetry.extraAttributes[1].baz", "puk");
        AttributesMap extraAttributes = underTest.getExtraAttributes();
        assertThat(extraAttributes).isNotEmpty();
        assertThat(extraAttributes.get(AttributeKey.stringKey("foo"))).isEqualTo("bar");
        assertThat(extraAttributes.get(AttributeKey.stringKey("baz"))).isEqualTo("puk");
    }

    @Test
    void should_get_empty_extra_attributes() {
        assertThat(underTest.getExtraAttributes()).isEmpty();
    }

    @Test
    void should_get_cert_and_keys() {
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.certs[0]", "/path/to/cert/1");
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.certs[1]", "/path/to/cert/2");
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.keys[0]", "/path/to/key/1");
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.keys[1]", "/path/to/key/2");

        assertThat(underTest.getKeystorePemCerts()).containsExactly("/path/to/cert/1", "/path/to/cert/2");
        assertThat(underTest.getKeystorePemKeys()).containsExactly("/path/to/key/1", "/path/to/key/2");
    }

    @Test
    void should_get_cert_and_from_fallback() {
        environment.setProperty("services.tracing.otel.ssl.keystore.certs[0]", "/path/to/cert/1");
        environment.setProperty("services.tracing.otel.ssl.keystore.certs[1]", "/path/to/cert/2");
        environment.setProperty("services.tracing.otel.ssl.keystore.keys[0]", "/path/to/key/1");
        environment.setProperty("services.tracing.otel.ssl.keystore.keys[1]", "/path/to/key/2");

        assertThat(underTest.getKeystorePemCerts()).containsExactly("/path/to/cert/1", "/path/to/cert/2");
        assertThat(underTest.getKeystorePemKeys()).containsExactly("/path/to/key/1", "/path/to/key/2");
    }

    @Test
    void should_get_empty_property_list() {
        assertThat(underTest.getKeystorePemCerts()).isEmpty();
        assertThat(underTest.getKeystorePemKeys()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getRedactionConfig
    // -------------------------------------------------------------------------

    @Nested
    class GetRedactionConfig {

        @Test
        void returns_empty_config_when_no_rules_defined() {
            assertThat(underTest.getRedactionConfig()).isSameAs(RedactionConfig.EMPTY);
        }

        @Test
        void reads_single_full_mask_rule() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.type", "FULL");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.replacement", "[HIDDEN]");

            RedactionConfig config = underTest.getRedactionConfig();

            assertThat(config.rules()).hasSize(1);
            RedactionRule rule = config.rules().get(0);
            assertThat(rule.attributeNamePattern()).isEqualTo("enduser.id");
            assertThat(rule.maskingStrategy()).isInstanceOf(FullMaskingStrategy.class);
            assertThat(((FullMaskingStrategy) rule.maskingStrategy()).replacement()).isEqualTo("[HIDDEN]");
        }

        @Test
        void full_rule_without_replacement_uses_DEFAULT_sentinel_so_config_defaultReplacement_applies() {
            // Critical: must produce MaskingStrategy.DEFAULT (==), not fullMask(), so the
            // config-level defaultReplacement is honoured at evaluation time in CompiledRedactionRule.
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.type", "FULL");
            environment.setProperty("services.opentelemetry.redactionDefaultReplacement", "[CONFIG_DEFAULT]");

            RedactionConfig config = underTest.getRedactionConfig();

            assertThat(config.rules().get(0).maskingStrategy()).isSameAs(MaskingStrategy.DEFAULT);
            assertThat(config.defaultReplacement()).isEqualTo("[CONFIG_DEFAULT]");
        }

        @Test
        void full_rule_with_no_masking_strategy_block_defaults_to_DEFAULT_sentinel() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");

            RedactionConfig config = underTest.getRedactionConfig();

            assertThat(config.rules().get(0).maskingStrategy()).isSameAs(MaskingStrategy.DEFAULT);
        }

        @Test
        void reads_partial_mask_rule() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "payment.card");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.type", "PARTIAL");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.prefixLength", "0");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.suffixLength", "4");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.replacement", "X");

            RedactionConfig config = underTest.getRedactionConfig();

            RedactionRule rule = config.rules().get(0);
            assertThat(rule.maskingStrategy()).isInstanceOf(PartialMaskingStrategy.class);
            PartialMaskingStrategy partial = (PartialMaskingStrategy) rule.maskingStrategy();
            assertThat(partial.prefixLength()).isZero();
            assertThat(partial.suffixLength()).isEqualTo(4);
            assertThat(partial.maskChar()).isEqualTo("X");
        }

        @Test
        void reads_value_pattern() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "*");
            environment.setProperty("services.opentelemetry.redactionRules[0].valuePattern", "(5[1-5][0-9]{14})");

            RedactionConfig config = underTest.getRedactionConfig();

            assertThat(config.rules().get(0).valuePattern()).isEqualTo("(5[1-5][0-9]{14})");
        }

        @Test
        void reads_multiple_rules_in_order() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");
            environment.setProperty("services.opentelemetry.redactionRules[0].maskingStrategy.type", "FULL");
            environment.setProperty("services.opentelemetry.redactionRules[1].attributeNamePattern", "payment.card");
            environment.setProperty("services.opentelemetry.redactionRules[1].maskingStrategy.type", "PARTIAL");
            environment.setProperty("services.opentelemetry.redactionRules[1].maskingStrategy.suffixLength", "4");

            RedactionConfig config = underTest.getRedactionConfig();

            assertThat(config.rules()).hasSize(2);
            assertThat(config.rules().get(0).attributeNamePattern()).isEqualTo("enduser.id");
            assertThat(config.rules().get(1).attributeNamePattern()).isEqualTo("payment.card");
        }

        @Test
        void stops_reading_rules_at_first_missing_index() {
            // rules[0] and [2] defined but [1] is missing — should read only [0]
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");
            environment.setProperty("services.opentelemetry.redactionRules[2].attributeNamePattern", "payment.card");

            RedactionConfig config = underTest.getRedactionConfig();

            assertThat(config.rules()).hasSize(1);
        }

        @Test
        void result_is_cached_across_calls() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");

            assertThat(underTest.getRedactionConfig()).isSameAs(underTest.getRedactionConfig());
        }

        @Test
        void merge_yaml_rules_with_explicit_rules() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");

            RedactionConfig yamlConfig = underTest.getRedactionConfig();
            RedactionConfig explicit = new RedactionConfig(java.util.List.of(new RedactionRule("payment.card")));

            RedactionConfig merged = yamlConfig.mergeWith(explicit);

            assertThat(merged.rules()).hasSize(2);
            assertThat(merged.rules().get(0).attributeNamePattern()).isEqualTo("enduser.id");
            assertThat(merged.rules().get(1).attributeNamePattern()).isEqualTo("payment.card");
        }

        @Test
        void merge_returns_yaml_config_when_explicit_is_empty() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");

            RedactionConfig yamlConfig = underTest.getRedactionConfig();
            RedactionConfig merged = yamlConfig.mergeWith(RedactionConfig.EMPTY);

            assertThat(merged).isSameAs(yamlConfig);
        }

        @Test
        void merge_returns_explicit_config_when_yaml_is_empty() {
            RedactionConfig explicit = new RedactionConfig(java.util.List.of(new RedactionRule("payment.card")));
            RedactionConfig merged = RedactionConfig.EMPTY.mergeWith(explicit);

            assertThat(merged).isSameAs(explicit);
        }

        @Test
        void merge_preserves_yaml_default_replacement_when_product_config_has_no_explicit_replacement() {
            environment.setProperty("services.opentelemetry.redactionRules[0].attributeNamePattern", "enduser.id");
            environment.setProperty("services.opentelemetry.redactionDefaultReplacement", "[YAML_DEFAULT]");

            RedactionConfig yamlConfig = underTest.getRedactionConfig();
            // Product config has no explicit defaultReplacement — compact constructor normalises to "[REDACTED]"
            RedactionConfig productConfig = new RedactionConfig(java.util.List.of(new RedactionRule("payment.card")));

            RedactionConfig merged = yamlConfig.mergeWith(productConfig);

            assertThat(merged.defaultReplacement()).isEqualTo("[YAML_DEFAULT]");
        }
    }
}
