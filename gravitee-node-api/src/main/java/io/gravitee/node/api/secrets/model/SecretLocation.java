package io.gravitee.node.api.secrets.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents where the secret is from a provider perspective. It is a map internally.
 *
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
        Objects.requireNonNull(key);
        return (T) properties.get(key);
    }

    public void put(String key, Object value) {
        Objects.requireNonNull(key);
        properties.put(key, value);
    }

    public <T> T getOrDefault(String key, String defaultValue) {
        Objects.requireNonNull(key);
        return (T) properties.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecretLocation that = (SecretLocation) o;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }
}
