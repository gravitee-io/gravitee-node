package io.gravitee.node.api.secrets.runtime.grant;

import io.gravitee.node.api.secrets.runtime.spec.ValueKind;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record RuntimeContext(boolean allowed, ValueKind allowKind, String field) {
    public static final String EL_VARIABLE = "runtime_secrets_context";
}
