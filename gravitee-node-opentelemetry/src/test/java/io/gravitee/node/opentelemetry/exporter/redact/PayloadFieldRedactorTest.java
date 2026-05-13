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

import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PayloadFormat;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingConfig;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingRule;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PayloadFieldRedactorTest {

    private static PayloadFieldRedactor redactorWith(PayloadMaskingRule... rules) {
        return new PayloadFieldRedactor(new PayloadMaskingConfig(List.of(rules)));
    }

    @Test
    void should_use_eventFormat_JSON_hint_and_apply_jsonpath_rule() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.AUTO)
        );
        String result = redactor.redact("{\"password\":\"secret\"}", "JSON", PayloadPhase.REQUEST);
        assertThat(result).contains("[REDACTED]").doesNotContain("secret");
    }

    @Test
    void should_use_eventFormat_XML_hint_and_apply_xpath_rule() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("//cvv", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.AUTO)
        );
        String result = redactor.redact("<order><cvv>123</cvv></order>", "XML", PayloadPhase.REQUEST);
        assertThat(result).contains("[REDACTED]").doesNotContain("123");
    }

    @Test
    void should_detect_json_by_body_heuristic_when_no_event_format() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.AUTO)
        );
        String result = redactor.redact("{\"password\":\"secret\"}", null, PayloadPhase.REQUEST);
        assertThat(result).contains("[REDACTED]");
    }

    @Test
    void should_detect_xml_by_body_heuristic_when_no_event_format() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("//cvv", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.AUTO)
        );
        String result = redactor.redact("<order><cvv>999</cvv></order>", null, PayloadPhase.REQUEST);
        assertThat(result).contains("[REDACTED]").doesNotContain("999");
    }

    @Test
    void should_return_original_for_unrecognised_format() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.AUTO)
        );
        String body = "plain text body";
        String result = redactor.redact(body, null, PayloadPhase.REQUEST);
        assertThat(result).isSameAs(body);
    }

    @Test
    void should_not_mask_response_body_with_request_only_rule() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.REQUEST, PayloadFormat.JSON)
        );
        String body = "{\"password\":\"secret\"}";
        String result = redactor.redact(body, "JSON", PayloadPhase.RESPONSE);
        assertThat(result).contains("secret");
    }

    @Test
    void should_mask_both_phases_with_both_rule() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.JSON)
        );
        assertThat(redactor.redact("{\"password\":\"s\"}", "JSON", PayloadPhase.REQUEST)).contains("[REDACTED]");
        assertThat(redactor.redact("{\"password\":\"s\"}", "JSON", PayloadPhase.RESPONSE)).contains("[REDACTED]");
    }

    @Test
    void should_apply_json_format_rule_to_json_body() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.token", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.JSON)
        );
        String result = redactor.redact("{\"token\":\"abc\"}", "JSON", PayloadPhase.REQUEST);
        assertThat(result).contains("[REDACTED]");
    }

    @Test
    void should_apply_xml_format_rule_to_xml_body() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("//token", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.XML)
        );
        String result = redactor.redact("<root><token>abc</token></root>", "XML", PayloadPhase.REQUEST);
        assertThat(result).contains("[REDACTED]");
    }

    @Test
    void should_not_apply_xml_rule_to_json_body() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("//token", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.XML)
        );
        String body = "{\"token\":\"abc\"}";
        String result = redactor.redact(body, "JSON", PayloadPhase.REQUEST);
        assertThat(result).contains("abc");
    }

    @Test
    void should_return_null_unchanged() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.JSON)
        );
        assertThat(redactor.redact(null, "JSON", PayloadPhase.REQUEST)).isNull();
    }

    @Test
    void should_return_blank_body_unchanged() {
        PayloadFieldRedactor redactor = redactorWith(
            new PayloadMaskingRule("$.password", MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.JSON)
        );
        assertThat(redactor.redact("   ", "JSON", PayloadPhase.REQUEST)).isEqualTo("   ");
    }

    @Test
    void should_return_original_when_no_rules_configured() {
        PayloadFieldRedactor redactor = new PayloadFieldRedactor(PayloadMaskingConfig.EMPTY);
        String body = "{\"password\":\"secret\"}";
        assertThat(redactor.redact(body, "JSON", PayloadPhase.REQUEST)).isSameAs(body);
    }
}
