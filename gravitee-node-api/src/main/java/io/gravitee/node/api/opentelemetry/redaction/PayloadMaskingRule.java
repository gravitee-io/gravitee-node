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

import java.util.Objects;

/**
 * An immutable rule that masks a specific field within a structured payload body.
 *
 * <p>Unlike {@link RedactionRule}, which matches OTel span/log <em>attribute keys</em> by name
 * pattern, this rule targets <em>field values</em> inside the body using a path expression:
 * <ul>
 *   <li>JSONPath — e.g. {@code $.creditCard.number}, {@code $.items[*].payment.cvv}</li>
 *   <li>XPath — e.g. {@code /order/payment/cvv}</li>
 * </ul>
 *
 * @param path            JSONPath or XPath expression identifying the field(s) to mask (required)
 * @param maskingStrategy how to mask the matched value; defaults to {@link MaskingStrategy#DEFAULT}
 * @param phase           which traffic direction this rule applies to; defaults to {@link PayloadPhase#BOTH}
 * @param format          body format expected by this rule; defaults to {@link PayloadFormat#AUTO}
 */
public record PayloadMaskingRule(String path, MaskingStrategy maskingStrategy, PayloadPhase phase, PayloadFormat format) {
    public PayloadMaskingRule {
        Objects.requireNonNull(path, "path must not be null");
        maskingStrategy = (maskingStrategy == null) ? MaskingStrategy.DEFAULT : maskingStrategy;
        phase = (phase == null) ? PayloadPhase.BOTH : phase;
        format = (format == null) ? PayloadFormat.AUTO : format;
    }

    public PayloadMaskingRule(String path) {
        this(path, MaskingStrategy.DEFAULT, PayloadPhase.BOTH, PayloadFormat.AUTO);
    }

    public PayloadMaskingRule(String path, MaskingStrategy maskingStrategy) {
        this(path, maskingStrategy, PayloadPhase.BOTH, PayloadFormat.AUTO);
    }

    public PayloadMaskingRule(String path, MaskingStrategy maskingStrategy, PayloadPhase phase) {
        this(path, maskingStrategy, phase, PayloadFormat.AUTO);
    }
}
