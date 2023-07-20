package io.gravitee.node.secrets.api.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class SecretEvent {

    private final SecretEvent.Type type;

    private final Secret secret;

    public Secret secret() {
        return this.secret;
    }

    public enum Type {
        CREATED,
        UPDATED,
        DELETED,
    }
}
