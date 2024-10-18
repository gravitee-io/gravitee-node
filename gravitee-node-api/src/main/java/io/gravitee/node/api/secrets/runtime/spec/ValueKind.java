package io.gravitee.node.api.secrets.runtime.spec;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum ValueKind {
    GENERIC,
    PASSWORD,
    HEADER,
    PRIVATE_KEY,
    PUBLIC_KEY,
    KEYSTORE,
}
