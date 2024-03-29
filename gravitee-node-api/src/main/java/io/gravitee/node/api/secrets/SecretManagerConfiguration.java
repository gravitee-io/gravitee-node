package io.gravitee.node.api.secrets;

/**
 * Base class to all secret manager configuration
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretManagerConfiguration {
    boolean isEnabled();
}
