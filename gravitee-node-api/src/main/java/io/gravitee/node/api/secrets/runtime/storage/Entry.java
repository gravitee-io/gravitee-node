package io.gravitee.node.api.secrets.runtime.storage;

import io.gravitee.node.api.secrets.model.Secret;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Entry(Type type, Map<String, Secret> value, String error) {
    public Entry {
        value = value != null ? new HashMap<>(value) : new HashMap<>();
    }
    public enum Type {
        VALUE,
        EMPTY,
        NOT_FOUND,
        ERROR,
    }
}
