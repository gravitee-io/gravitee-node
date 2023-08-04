package io.gravitee.node.secrets.api.model;

/**
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
