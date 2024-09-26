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
            throw new IllegalArgumentException("naturalId and replacement list don't match in size");
        }

        for (int i = 0; i < rawRefs.size(); i++) {
            String replacement = expressions.get(i);

            // replace naturalId by expression
            Position position = rawRefs.get(i).position;
            payload.replace(position.start, position.end, replacement);

            // compute lengthDiff in position
            int refStringLength = position.end - position.start;
            int replacementLength = replacement.length();
            int lengthDiff = replacementLength - refStringLength;
            // apply offset change on next naturalId positions
            for (int p = i + 1; p < expressions.size(); p++) {
                rawRefs.get(p).position.move(lengthDiff);
            }
        }

        return payload.toString();
    }
}
