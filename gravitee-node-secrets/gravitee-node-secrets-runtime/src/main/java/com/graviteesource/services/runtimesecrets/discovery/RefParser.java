package com.graviteesource.services.runtimesecrets.discovery;

import static io.gravitee.node.api.secrets.runtime.discovery.Ref.URI_KEY_SEPARATOR;

import com.graviteesource.services.runtimesecrets.errors.SecretRefParsingException;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RefParser {

    public static final String BEGIN_SEPARATOR = "<<";
    public static final String END_SEPARATOR = ">>";
    private static final String NAME_TYPE = " name ";
    private static final int MAX_MAIN_TYPE_SIZE = NAME_TYPE.length();
    private static final String URI_TYPE = " uri ";
    private static final String KEY_TYPE = " key ";

    private static final List<String> AFTER_MAIN_TOKENS = List.of(NAME_TYPE, URI_TYPE, KEY_TYPE, END_SEPARATOR);

    private static final char EL_START_CB = '{';
    private static final char EL_START_HASH = '#';
    private static final List<String> EL_START = List.of("" + EL_START_CB + EL_START_HASH, "" + EL_START_HASH);

    /**
     * <p><code><< [name|uri] [EL_]EXPRESSION [[name | uri | key] [EL_]EXPRESSION] >></code></p>
     *
     * <code>&lt;space+></code> = one or several spaces (\<code>u0020</code>)<br>
     * <code>BEGIN_SEPARATOR</code> "<code><<</code>" optionally followed by <code><space></code><br>
     * <code>END_SEPARATOR</code> "<code>>></code>" optionally preceded by <code><space></code><br>
     * <code>MAIN_TYPE</code> is literal case-sensitive string: "<code>uri</code>" or "<code>name</code>" preceded AND followed by <code><space></code><br>
     * <code>OPTIONAL_TYPE</code> extends <code>MAIN_TYPE</code> with literal "<code>key</code>"<br>
     * <code>KEY</code> is a string or an <code>EL_STRING</code> that designate the key to get from a secret map<br>
     * <code>EXPRESSION</code> is any string NOT starting with '<code>#</code>' or "<code>{#</code>". When <code>MAIN_TYPE</code>is absent or "<code>uri</code>" then a <code>EL_EXPRESSION</code> ending with "<code>:</code>" and followed  is an alias of "<code>key KEY</code>"<br>
     * <code>EL_EXPRESSION</code> = string starting with '<code>#</code>' or "<code>{#</code>". After parsing <code>{</code> and <code>}</code> is removed (a.k.a mixin).<br>
     *
     * @param ref the full naturalId (with start and end separator
     * @return a SecretRef
     * @throws SecretRefParsingException when parsing fails
     */
    public static Ref parse(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new SecretRefParsingException("naturalId is null or empty");
        }
        var buffer = new StringBuilder(ref);
        // delete
        buffer.delete(0, BEGIN_SEPARATOR.length());

        final String typeString = mainType(buffer);
        final Ref.MainType mainType;
        try {
            mainType = Ref.MainType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw enumError("unknown kind: '%s' for secret reference '%s'".formatted(typeString, ref));
        }
        final RefParsing refParsing = mainExpression(buffer);

        Ref.SecondaryType secondaryType = null;
        if (refParsing.secondaryType() != null) {
            try {
                secondaryType = Ref.SecondaryType.valueOf(refParsing.secondaryType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw enumError("unknown kind: '%s' for secret reference '%s'".formatted(refParsing.secondaryType(), ref));
            }
            if (mainType == Ref.MainType.URI && secondaryType != Ref.SecondaryType.KEY) {
                throw new SecretRefParsingException(
                    "reference of kind '%s' can only be followed by keyword '%s' or contain '%s' in reference %s".formatted(
                            URI_TYPE.trim(),
                            KEY_TYPE.trim(),
                            URI_KEY_SEPARATOR,
                            ref
                        )
                );
            }
            if (isEL(refParsing.mainExpression)) {
                throw new SecretRefParsingException(
                    "reference of kind '%s' is using an EL expression, it cannot be followed a keyword in reference %s".formatted(
                            URI_TYPE.trim(),
                            ref
                        )
                );
            }
        }

        return new Ref(
            mainType,
            toExpression(refParsing.mainExpression()),
            secondaryType,
            secondaryType != null ? toExpression(refParsing.secondaryExpression()) : null,
            ref
        );
    }

    private static String mainType(StringBuilder buffer) {
        while (buffer.charAt(0) == ' ') {
            buffer.delete(0, 1);
        }
        int typeEnd = buffer.indexOf(" ");
        // reach end without spaces or bigger than a main kind
        if (typeEnd == -1 || typeEnd > MAX_MAIN_TYPE_SIZE) {
            if (buffer.charAt(0) == SecretURL.URL_SEPARATOR) {
                return URI_TYPE.trim();
            }
            if (isEL(buffer.toString())) {
                throw new SecretRefParsingException(
                    "EL expression must be preceded by '%s' or '%s' when starting the secret reference".formatted(
                            NAME_TYPE.stripLeading(),
                            URI_TYPE.stripLeading()
                        )
                );
            }
            return NAME_TYPE.trim();
        }
        String type = buffer.substring(0, typeEnd);
        buffer.delete(0, typeEnd);
        return type;
    }

    record RefParsing(String mainExpression, String secondaryType, String secondaryExpression) {}

    private static RefParsing mainExpression(StringBuilder buffer) {
        String foundToken = null;
        String expression = null;
        int end = 0;
        for (String token : AFTER_MAIN_TOKENS) {
            end = buffer.indexOf(token);
            if (end != -1) {
                foundToken = token;
                expression = buffer.substring(0, end);
                buffer.delete(0, end);
                break;
            }
        }

        if (foundToken == null) {
            throw new SecretRefParsingException("reference %s syntax is incorrect looks like nothing is specified");
        }

        String uriOrName = expression.trim();
        String secondaryType = null;
        String secondary = null;

        if (!foundToken.equals(END_SEPARATOR)) {
            secondary = buffer.substring(foundToken.length(), buffer.length() - END_SEPARATOR.length()).trim();
            secondaryType = foundToken.trim();
        }
        if (!foundToken.equals(KEY_TYPE) && expression.contains(URI_KEY_SEPARATOR)) {
            UriAndKey uriAndKey = parseUriAndKey(expression, end);
            uriOrName = uriAndKey.uri();
            secondaryType = KEY_TYPE.trim();
            secondary = uriAndKey.key();
        }

        return new RefParsing(uriOrName, secondaryType, secondary);
    }

    public record UriAndKey(String uri, String key) {
        public Ref asRef() {
            return new Ref(
                Ref.MainType.URI,
                new Ref.Expression(uri, false),
                Ref.SecondaryType.KEY,
                new Ref.Expression(key, false),
                asString()
            );
        }

        private String asString() {
            return Ref.formatUriAndKey(uri, key);
        }
    }

    public static UriAndKey parseUriAndKey(String expression, int end) {
        int keyIndex = expression.indexOf(URI_KEY_SEPARATOR);
        String uri = expression.substring(0, keyIndex).trim();
        String key = expression.substring(keyIndex + 1, end).trim();
        return new UriAndKey(uri, key);
    }

    private static boolean isEL(String buffer) {
        if (buffer == null || buffer.isBlank()) {
            return false;
        }
        for (String start : EL_START) {
            if (buffer.indexOf(start) == 0) {
                return true;
            }
        }
        return false;
    }

    private static Ref.Expression toExpression(String spec) {
        if (isEL(spec)) {
            return new Ref.Expression(cleanEL(spec), true);
        }
        return new Ref.Expression(spec, false);
    }

    private static String cleanEL(String el) {
        if (el.charAt(0) == EL_START_CB) {
            return el.substring(1, el.length() - 1);
        }
        return el;
    }

    private static SecretRefParsingException enumError(String typeString) {
        return new SecretRefParsingException(typeString);
    }
}
