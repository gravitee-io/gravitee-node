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
package com.graviteesource.services.runtimesecrets.discovery;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public class PayloadRefParser {

    private final StringBuilder payload;

    @Getter(AccessLevel.PACKAGE)
    private List<RawSecretRef> rawRefs;

    public PayloadRefParser(String payload) {
        this.payload = new StringBuilder(payload);
    }

    @AllArgsConstructor
    static class Position {

        private int start;
        private int end;

        private void move(int quantity) {
            start += quantity;
            end += quantity;
        }
    }

    public record RawSecretRef(String ref, Position position) {}

    public List<RawSecretRef> runDiscovery() {
        int start = 0;
        List<RawSecretRef> result = new ArrayList<>();
        do {
            start = payload.indexOf(RefParser.BEGIN_SEPARATOR, start);
            if (start >= 0) {
                int end = payload.indexOf(RefParser.END_SEPARATOR, start);
                int afterEnd = end + RefParser.END_SEPARATOR.length();
                String ref = payload.substring(start, afterEnd);
                result.add(new RawSecretRef(ref, new Position(start, afterEnd)));
                start = afterEnd;
            }
        } while (start >= 0);
        this.rawRefs = result;
        return result;
    }

    public String replaceRefs(List<String> expressions) {
        if (expressions.size() != this.rawRefs.size()) {
            throw new IllegalArgumentException("ref and replacement list don't match in size");
        }

        for (int i = 0; i < rawRefs.size(); i++) {
            String replacement = expressions.get(i);

            // replace ref by expression
            Position position = rawRefs.get(i).position;
            payload.replace(position.start, position.end, replacement);

            // compute lengthDiff in position
            int refStringLength = position.end - position.start;
            int replacementLength = replacement.length();
            int lengthDiff = replacementLength - refStringLength;
            // apply offset change on next ref positions
            for (int p = i + 1; p < expressions.size(); p++) {
                rawRefs.get(p).position.move(lengthDiff);
            }
        }

        return payload.toString();
    }

    public String getUpdatePayload() {
        return payload.toString();
    }
}
