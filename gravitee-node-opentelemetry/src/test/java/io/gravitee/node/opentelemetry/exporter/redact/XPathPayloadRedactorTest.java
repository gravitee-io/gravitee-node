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
class XPathPayloadRedactorTest {

    private static final String DEFAULT_REPLACEMENT = "[REDACTED]";
    private final XPathPayloadRedactor redactor = new XPathPayloadRedactor();

    private CompiledPayloadMaskingRule compiled(String xpath) {
        return new CompiledPayloadMaskingRule(
            new PayloadMaskingRule(xpath, MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.XML),
            DEFAULT_REPLACEMENT
        );
    }

    private CompiledPayloadMaskingRule compiledPartial(String xpath, MaskingStrategy strategy) {
        return new CompiledPayloadMaskingRule(
            new PayloadMaskingRule(xpath, strategy, PayloadPhase.BOTH, PayloadFormat.XML),
            DEFAULT_REPLACEMENT
        );
    }

    @Test
    void should_mask_single_element() {
        String xml = "<order><payment><cvv>123</cvv></payment></order>";
        String result = redactor.redact(xml, List.of(compiled("//cvv")), PayloadPhase.REQUEST);
        assertThat(result).contains("[REDACTED]").doesNotContain("123");
    }

    @Test
    void should_mask_repeated_elements() {
        String xml = "<orders><order><cvv>111</cvv></order><order><cvv>222</cvv></order></orders>";
        String result = redactor.redact(xml, List.of(compiled("//cvv")), PayloadPhase.REQUEST);
        assertThat(result).doesNotContain("111").doesNotContain("222");
        assertThat(result.split("\\[REDACTED\\]", -1).length - 1).isEqualTo(2);
    }

    @Test
    void should_apply_partial_masking_strategy() {
        String xml = "<order><card>4111111111111111</card></order>";
        MaskingStrategy partial = MaskingStrategy.partialMask(4, 4);
        String result = redactor.redact(xml, List.of(compiledPartial("//card", partial)), PayloadPhase.REQUEST);
        assertThat(result).contains("4111").contains("1111").doesNotContain("4111111111111111");
    }

    @Test
    void should_return_original_when_xpath_does_not_match() {
        String xml = "<order><user>alice</user></order>";
        String result = redactor.redact(xml, List.of(compiled("//cvv")), PayloadPhase.REQUEST);
        assertThat(result).contains("alice");
    }

    @Test
    void should_return_original_when_rules_list_is_empty() {
        String xml = "<order><cvv>123</cvv></order>";
        String result = redactor.redact(xml, List.of(), PayloadPhase.REQUEST);
        assertThat(result).isSameAs(xml);
    }

    @Test
    void should_return_original_on_malformed_xml_fail_open() {
        String notXml = "not xml at all";
        String result = redactor.redact(notXml, List.of(compiled("//cvv")), PayloadPhase.REQUEST);
        assertThat(result).isSameAs(notXml);
    }

    @Test
    void should_skip_rule_that_does_not_apply_to_phase() {
        String xml = "<order><cvv>123</cvv></order>";
        CompiledPayloadMaskingRule requestOnlyRule = new CompiledPayloadMaskingRule(
            new PayloadMaskingRule("//cvv", MaskingStrategy.DEFAULT, PayloadPhase.REQUEST, PayloadFormat.XML),
            DEFAULT_REPLACEMENT
        );
        String result = redactor.redact(xml, List.of(requestOnlyRule), PayloadPhase.RESPONSE);
        assertThat(result).contains("123");
    }
}
