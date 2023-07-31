package io.gravitee.node.secrets.api.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    private final Instant expiresAt;

    public boolean isEmpty() {
        return value == null || value.length == 0;
    }

    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public String asString() {
        return new String(value(), StandardCharsets.UTF_8);
    }
}
