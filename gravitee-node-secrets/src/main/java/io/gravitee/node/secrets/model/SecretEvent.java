package io.gravitee.node.secrets.model;

import java.util.Optional;
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

    public Optional<Secret> secret() {
        return Optional.ofNullable(this.secret);
    }

    public enum Type {
        CREATED,
        UPDATED,
        DELETED,
    }
}
