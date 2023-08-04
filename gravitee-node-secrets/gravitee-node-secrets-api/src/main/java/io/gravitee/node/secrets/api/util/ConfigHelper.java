package io.gravitee.node.secrets.api.util;

import io.gravitee.node.secrets.api.model.Secret;
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

    public static Map<String, Object> removePrefix(Map<String, Object> propertiesMap, String prefix) {
        return propertiesMap
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix))
            // chopping the prefix out of the key
            .collect(Collectors.toMap(e -> e.getKey().substring(prefix.length() + 1), Map.Entry::getValue));
    }
}
