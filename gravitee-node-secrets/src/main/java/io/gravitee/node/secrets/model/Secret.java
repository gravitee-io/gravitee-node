package io.gravitee.node.secrets.model;

import java.util.Date;
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
public final class Secret {

    private final SecretAccessContext context;
    private final String name;
    private final Date expiresAt;
    private final byte[] value;

    public Optional<Date> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
