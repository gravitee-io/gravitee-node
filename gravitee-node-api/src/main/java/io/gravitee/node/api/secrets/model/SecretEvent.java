package io.gravitee.node.api.secrets.model;

/**
 * This record represent an event occurred during a secret watch.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record SecretEvent(SecretEvent.Type type, SecretMap secretMap) {
    public enum Type {
        CREATED,
        UPDATED,
        DELETED,
    }
}
