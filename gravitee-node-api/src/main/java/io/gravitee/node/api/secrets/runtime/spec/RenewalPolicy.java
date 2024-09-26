package io.gravitee.node.api.secrets.runtime.spec;

import java.time.Duration;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record RenewalPolicy(Type type, Duration duration, Duration checkBeforeTTL) {
    public enum Type {
        NONE,
        TTL,
        POLL,
    }
}
