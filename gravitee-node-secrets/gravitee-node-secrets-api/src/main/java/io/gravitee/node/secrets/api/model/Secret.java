package io.gravitee.node.secrets.api.model;

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

    private final byte[] value;
    private final Date expiresAt;

    public boolean isEmpty() {
        return value == null || value.length == 0;
    }

    public Optional<Date> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
