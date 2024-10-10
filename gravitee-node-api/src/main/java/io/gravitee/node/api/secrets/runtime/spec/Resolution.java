package io.gravitee.node.api.secrets.runtime.spec;

import java.time.Duration;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Resolution(Type type, Duration pollInterval) {
    public enum Type {
        ONCE,
        POLL,
    }
}
