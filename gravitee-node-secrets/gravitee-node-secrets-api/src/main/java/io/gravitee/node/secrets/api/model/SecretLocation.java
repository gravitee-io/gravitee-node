package io.gravitee.node.secrets.api.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretLocation {

    private final Map<String, Object> properties;

    private SecretLocation(Map<String, Object> properties, boolean copyMap) {
        if (copyMap) {
            this.properties = new HashMap<>(properties);
        } else {
            this.properties = properties;
        }
    }

    public SecretLocation(Map<String, Object> properties) {
        this(properties, true);
    }

    public SecretLocation() {
        this(new HashMap<>(), false);
    }

    public <T> T get(String key) {
        return (T) properties.get(key);
    }

    public <T> T put(String key, Object value) {
        return (T) properties.put(key, value);
    }

    public <T> T getOrDefault(String key, String defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    public SecretLocation sealed() {
        return new SecretLocation(Collections.unmodifiableMap(properties), false);
    }
}
