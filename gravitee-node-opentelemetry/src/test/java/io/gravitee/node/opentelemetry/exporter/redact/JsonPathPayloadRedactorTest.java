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
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingRule;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JsonPathPayloadRedactorTest {

    private static final String DEFAULT_REPLACEMENT = "[REDACTED]";
    private final JsonPathPayloadRedactor redactor = new JsonPathPayloadRedactor();

    private CompiledPayloadMaskingRule compiled(String path) {
        return new CompiledPayloadMaskingRule(new PayloadMaskingRule(path), DEFAULT_REPLACEMENT);
    }

    private CompiledPayloadMaskingRule compiled(String path, MaskingStrategy strategy) {
        return new CompiledPayloadMaskingRule(new PayloadMaskingRule(path, strategy), DEFAULT_REPLACEMENT);
    }

    private CompiledPayloadMaskingRule compiledForPhase(String path, PayloadPhase phase) {
        return new CompiledPayloadMaskingRule(
            new PayloadMaskingRule(path, MaskingStrategy.DEFAULT, phase, PayloadFormat.JSON),
            DEFAULT_REPLACEMENT
        );
    }

    @Test
    void should_mask_top_level_field() {
        String json = "{\"password\":\"secret\",\"user\":\"alice\"}";
        String result = redactor.redact(json, List.of(compiled("$.password")), PayloadPhase.REQUEST);
        assertThat(result).contains("\"password\":\"[REDACTED]\"").contains("\"user\":\"alice\"");
    }

    @Test
    void should_mask_nested_field() {
        String json = "{\"card\":{\"number\":\"4111111111111111\",\"cvv\":\"123\"}}";
        String result = redactor.redact(json, List.of(compiled("$.card.cvv")), PayloadPhase.REQUEST);
        assertThat(result).contains("\"cvv\":\"[REDACTED]\"").contains("\"number\":\"4111111111111111\"");
    }

    @Test
    void should_mask_all_elements_in_array() {
        String json = "{\"items\":[{\"payment\":{\"cvv\":\"111\"}},{\"payment\":{\"cvv\":\"222\"}}]}";
        String result = redactor.redact(json, List.of(compiled("$.items[*].payment.cvv")), PayloadPhase.REQUEST);
        assertThat(result).doesNotContain("111").doesNotContain("222").contains("[REDACTED]");
    }

    @Test
    void should_apply_partial_masking_strategy() {
        String json = "{\"card\":\"4111111111111111\"}";
        MaskingStrategy partial = MaskingStrategy.partialMask(4, 4);
        String result = redactor.redact(json, List.of(compiled("$.card", partial)), PayloadPhase.REQUEST);
        assertThat(result).contains("4111").contains("1111").doesNotContain("4111111111111111");
    }

    @Test
    void should_return_original_when_path_does_not_match() {
        String json = "{\"user\":\"alice\"}";
        String result = redactor.redact(json, List.of(compiled("$.password")), PayloadPhase.REQUEST);
        assertThat(result).isSameAs(json);
    }

    @Test
    void should_return_original_when_rules_list_is_empty() {
        String json = "{\"password\":\"secret\"}";
        String result = redactor.redact(json, List.of(), PayloadPhase.REQUEST);
        assertThat(result).isSameAs(json);
    }

    @Test
    void should_return_original_on_malformed_json_fail_open() {
        String notJson = "not-json-at-all";
        String result = redactor.redact(notJson, List.of(compiled("$.password")), PayloadPhase.REQUEST);
        assertThat(result).isSameAs(notJson);
    }

    @Test
    void should_skip_rule_that_does_not_apply_to_phase() {
        String json = "{\"password\":\"secret\"}";
        CompiledPayloadMaskingRule requestOnlyRule = compiledForPhase("$.password", PayloadPhase.REQUEST);
        String result = redactor.redact(json, List.of(requestOnlyRule), PayloadPhase.RESPONSE);
        assertThat(result).isSameAs(json);
    }

    @Test
    void should_mask_when_phase_is_both() {
        String json = "{\"password\":\"secret\"}";
        CompiledPayloadMaskingRule bothRule = compiledForPhase("$.password", PayloadPhase.BOTH);
        String result = redactor.redact(json, List.of(bothRule), PayloadPhase.RESPONSE);
        assertThat(result).contains("[REDACTED]");
    }

    @Test
    void should_return_original_when_body_is_blank() {
        String result = redactor.redact("   ", List.of(compiled("$.password")), PayloadPhase.REQUEST);
        assertThat(result).isEqualTo("   ");
    }
}
