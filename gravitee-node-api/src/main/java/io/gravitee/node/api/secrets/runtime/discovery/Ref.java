package io.gravitee.node.api.secrets.runtime.discovery;

import io.gravitee.node.api.secrets.runtime.spec.Spec;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public record Ref(
    MainType mainType,
    Expression mainExpression,
    SecondaryType secondaryType,
    Expression secondaryExpression,
    String rawRef
) {
    public record Expression(String value, boolean isEL) {
        public boolean isLiteral() {
            return !isEL;
        }
    }

    public enum MainType {
        NAME,
        URI,
    }

    public enum SecondaryType {
        NAME,
        URI,
        KEY,
    }

    public static final String URI_KEY_SEPARATOR = ":";

    public Spec toRuntimeSpec(String envId) {
        return new Spec(null, null, mainExpression.value(), secondaryExpression().value(), null, false, true, null, null, envId);
    }

    /**
     * Check type are compatible and call {@link #formatUriAndKey(String, String)}
     * @return main value and secondary value with key alias separator
     */
    public String uriAndKey() {
        if (mainType == MainType.URI && secondaryType == SecondaryType.KEY) {
            return formatUriAndKey(mainExpression.value(), secondaryExpression.value());
        }
        throw new IllegalArgumentException(
            "cannot format main is %s and secondary is %s it should %s and %s".formatted(
                    mainType,
                    secondaryType,
                    MainType.URI,
                    SecondaryType.KEY
                )
        );
    }

    /**
     * @return main value and secondary value with key alias separator
     */
    public static String formatUriAndKey(String uri, String key) {
        return uri + URI_KEY_SEPARATOR + key;
    }
}
