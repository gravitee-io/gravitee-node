package io.gravitee.node.secrets.api.util;

import io.gravitee.node.secrets.api.model.Secret;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigHelper {

    public static String getStringOrSecret(Map<String, Object> propertiesMap, String propertyName, String defaultValue) {
        return secretAsStringOrCast(propertiesMap.getOrDefault(propertyName, defaultValue));
    }

    public static String getStringOrSecret(Map<String, Object> propertiesMap, String propertyName) {
        Object data = Objects.requireNonNull(propertiesMap.get(propertyName));
        return secretAsStringOrCast(data);
    }

    private static String secretAsStringOrCast(Object data) {
        if (data instanceof Secret secret) {
            return secret.asString();
        }
        return (String) data;
    }

    /**
     * Find an {@link Enum} literal from a String literal regardless of the case.
     *
     * @param literal      the uncased string to lookup
     * @param enumClass    the Enum class where to lookup for the literal
     * @param propertyName the config property name (used for errors only)
     * @param <T>          Type of Enum to avoid casting
     * @return an enum literal or throws an {@link IllegalArgumentException}
     */
    public static <T extends Enum<T>> T enumValueOfIgnoreCase(String literal, Class<T> enumClass, String propertyName) {
        EnumSet<T> values = EnumSet.allOf(enumClass);

        for (T value : values) {
            if (value.name().equalsIgnoreCase(literal)) {
                return value;
            }
        }
        throw new IllegalArgumentException(
            "Invalid value for %s: %s. Possible values: '%s'".formatted(
                    propertyName,
                    literal,
                    values.stream().map(Enum::name).map(String::toLowerCase).collect(Collectors.joining("', '"))
                )
        );
    }

    public static Map<String, Object> removePrefix(Map<String, Object> propertiesMap, String prefix) {
        return propertiesMap
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .filter(e -> Objects.nonNull(e.getValue()))
            // chopping the prefix out of the key
            .collect(Collectors.toMap(e -> e.getKey().substring(prefix.length() + 1), Map.Entry::getValue));
    }
}
