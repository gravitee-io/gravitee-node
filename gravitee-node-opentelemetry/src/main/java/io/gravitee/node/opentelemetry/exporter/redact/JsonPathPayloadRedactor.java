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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Option;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies {@link CompiledPayloadMaskingRule} entries to a JSON body string using Jayway JsonPath.
 *
 * <p>Fail-open: any parse error returns the original body unchanged — the span is never dropped.
 * Thread-safe and stateless (no mutable instance fields).
 */
final class JsonPathPayloadRedactor {

    private static final Logger log = LoggerFactory.getLogger(JsonPathPayloadRedactor.class);

    // SUPPRESS_EXCEPTIONS: missing paths are silently skipped (no PathNotFoundException).
    // DEFAULT_PATH_LEAF_TO_NULL intentionally excluded — it would create null fields for non-existent paths.
    private static final Configuration JSONPATH_CONFIG = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build();

    /**
     * Applies all rules that match {@code phase} to {@code json} and returns the masked result.
     * Returns {@code json} unchanged if no rule matches or if the body cannot be parsed.
     */
    String redact(String json, List<CompiledPayloadMaskingRule> rules, PayloadPhase phase) {
        if (json == null || json.isBlank() || rules.isEmpty()) {
            return json;
        }
        DocumentContext ctx;
        try {
            ctx = com.jayway.jsonpath.JsonPath.using(JSONPATH_CONFIG).parse(json);
        } catch (Exception e) {
            log.warn("PayloadMasking: failed to parse JSON body — returning original body unchanged. Cause: {}", e.getMessage());
            return json;
        }

        boolean anyApplied = false;
        for (CompiledPayloadMaskingRule rule : rules) {
            if (!rule.appliesToPhase(phase) || rule.compiledJsonPath == null) {
                continue;
            }
            try {
                boolean[] matched = { false };
                ctx.map(
                    rule.compiledJsonPath,
                    (value, cfg) -> {
                        if (value == null) return null;
                        matched[0] = true;
                        return rule.applyMask(value.toString());
                    }
                );
                anyApplied |= matched[0];
            } catch (Exception e) {
                log.warn(
                    "PayloadMasking: failed to apply JSONPath '{}' — skipping rule. Cause: {}",
                    rule.compiledJsonPath.getPath(),
                    e.getMessage()
                );
            }
        }
        if (!anyApplied) {
            return json;
        }
        try {
            return ctx.jsonString();
        } catch (Exception e) {
            log.warn("PayloadMasking: failed to serialize masked JSON — returning original body unchanged. Cause: {}", e.getMessage());
            return json;
        }
    }
}
