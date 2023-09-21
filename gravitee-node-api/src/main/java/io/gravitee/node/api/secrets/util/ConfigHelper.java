package io.gravitee.node.api.secrets.util;

import io.gravitee.node.api.secrets.model.Secret;
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

    /**
     * Get a {@link Secret} and convert it to a string, or return the String for a given key
     *
     * @param propertiesMap the property map
     * @param propertyName  the property
     * @param defaultValue  a default value if the property is not found
     * @return a converted secret, the string or the default value
     */
    public static String getSecretAsString(Map<String, Object> propertiesMap, String propertyName, String defaultValue) {
        return secretAsStringOrCast(propertiesMap.getOrDefault(propertyName, defaultValue));
    }

    /**
     * Get a {@link Secret} and convert it to a string, or return the String for a given key, or it fails
     *
     * @param propertiesMap the property map
     * @param propertyName  the property
     * @return a converted secret
     */
    public static String getSecretAsString(Map<String, Object> propertiesMap, String propertyName) {
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

    /**
     * Keep entries starting with a given prefix from a property map but remove the prefix.
     * <p>Examples for 'auth' as prefix.</p>
     *
     * <ul>
     *     <li><code>auth.method        => method</code></li>
     *     <li><code>auth.basic.username => basic.username</code></li>
     *     <li><code>hello.world => [skipped]</code></li>
     * </ul>
     *
     * @param propertiesMap the map to filter out
     * @param prefix        the prefix to match
     * @return a new map containing filtered properties without the prefix
     */
    public static Map<String, Object> removePrefix(Map<String, Object> propertiesMap, String prefix) {
        return propertiesMap
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .filter(e -> Objects.nonNull(e.getValue()))
            // chopping the prefix out of the key
            .collect(Collectors.toMap(e -> e.getKey().substring(prefix.length() + 1), Map.Entry::getValue));
    }

    /**
     * Get a typed value out a map. If the map contains a String it will try to convert it into the desired type.
     *
     * @param map      the map to extract property of
     * @param property the property to extract
     * @param type     the desired type as a class
     * @param <T>      the desired type
     * @return a value
     * @throws IllegalArgumentException if a conversion cannot happen or if the property is not present
     */
    public static <T> T getProperty(Map<String, Object> map, String property, Class<T> type) {
        return getProperty(map, property, type, true);
    }

    /**
     * Get a typed value out a map or return the default value. If the map contains a String it will try to convert it into the desired type.
     *
     * @param map          the map to extract property of
     * @param property     the property to extract
     * @param type         the desired type as a class
     * @param defaultValue a default value if the property do not exist
     * @param <T>          the desired type
     * @return a value
     * @throws IllegalArgumentException if a conversion cannot happen
     */
    public static <T> T getProperty(Map<String, Object> map, String property, Class<T> type, T defaultValue) {
        T val = getProperty(map, property, type, false);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    private static <T> T getProperty(Map<String, Object> map, String property, Class<T> type, boolean failOnNull) {
        Objects.requireNonNull(map);

        Object value = map.get(property);
        if (failOnNull) {
            Objects.requireNonNull(value, "property '%s' must not be null".formatted(property));
        } else if (value == null) {
            return null;
        }

        // no need to convert
        if (value.getClass().equals(type)) {
            return (T) value;
        }

        if (value instanceof String str) {
            switch (type.getName()) {
                case "java.lang.Boolean" -> {
                    return (T) Boolean.valueOf(str);
                }
                case "java.lang.Integer" -> {
                    return (T) Integer.valueOf(str);
                }
                case "java.lang.Long" -> {
                    return (T) Long.valueOf(str);
                }
                default -> throw new IllegalArgumentException(
                    "Cannot convert property '%s' to type: %s".formatted(property, type.getName())
                );
            }
        }
        throw new IllegalArgumentException("Cannot convert property '%s' of type: %s".formatted(property, value.getClass().getName()));
    }
}
