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

import io.gravitee.node.api.opentelemetry.redaction.FullMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PartialMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.RedactionRule;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class CompiledRedactionRule {

    private static final String REGEX_PREFIX = "regex:";
    private static final Pattern GLOB_WILDCARD = Pattern.compile("\\*\\*|\\*");

    private final Pattern keyPattern;
    private final Pattern valuePattern;
    private final MaskingStrategy maskingStrategy;
    private final String effectiveFullMaskReplacement;

    CompiledRedactionRule(RedactionRule rule, String configDefaultReplacement) {
        this.maskingStrategy = rule.maskingStrategy();
        this.effectiveFullMaskReplacement =
            switch (rule.maskingStrategy()) {
                case FullMaskingStrategy full -> (full == MaskingStrategy.DEFAULT) ? configDefaultReplacement : full.replacement();
                case PartialMaskingStrategy partial -> null;
            };

        String raw = rule.attributeNamePattern();
        if (isShortName(raw)) {
            raw = REGEX_PREFIX + "(.*[._])?" + Pattern.quote(raw) + "$";
        }
        String keyRegex = raw.startsWith(REGEX_PREFIX) ? raw.substring(REGEX_PREFIX.length()) : globToRegex(raw);
        try {
            this.keyPattern = Pattern.compile(keyRegex, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                "Invalid key pattern '" + rule.attributeNamePattern() + "' in RedactionRule: " + e.getMessage(),
                e
            );
        }

        if (rule.valuePattern() != null) {
            try {
                this.valuePattern = Pattern.compile(rule.valuePattern());
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException(
                    "Invalid value pattern '" +
                    rule.valuePattern() +
                    "' in RedactionRule for key '" +
                    rule.attributeNamePattern() +
                    "': " +
                    e.getMessage(),
                    e
                );
            }
        } else {
            this.valuePattern = null;
        }
    }

    private static boolean isShortName(String s) {
        return !s.contains(".") && !s.contains("*") && !s.startsWith(REGEX_PREFIX);
    }

    boolean shouldRedact(String attributeKey, String attributeValue) {
        if (!keyPattern.matcher(attributeKey).matches()) {
            return false;
        }
        if (valuePattern == null) {
            return true;
        }
        if (attributeValue == null) {
            return false;
        }
        return valuePattern.matcher(attributeValue).find();
    }

    String applyRedaction(String value) {
        return switch (maskingStrategy) {
            case FullMaskingStrategy full -> effectiveFullMaskReplacement;
            case PartialMaskingStrategy partial -> {
                String maskChar = partial.maskChar();
                if (value == null) yield maskChar;
                int prefix = partial.prefixLength();
                int suffix = partial.suffixLength();
                if (value.length() <= prefix + suffix) yield maskChar.repeat(value.length());
                yield value.substring(0, prefix) +
                maskChar.repeat(value.length() - prefix - suffix) +
                value.substring(value.length() - suffix);
            }
        };
    }

    private static String globToRegex(String glob) {
        var sb = new StringBuilder("^");
        var m = GLOB_WILDCARD.matcher(glob);
        int last = 0;
        while (m.find()) {
            sb.append(Pattern.quote(glob.substring(last, m.start())));
            sb.append(m.end() - m.start() == 2 ? ".*" : "[^.]*");
            last = m.end();
        }
        sb.append(Pattern.quote(glob.substring(last)));
        return sb.append("$").toString();
    }
}
