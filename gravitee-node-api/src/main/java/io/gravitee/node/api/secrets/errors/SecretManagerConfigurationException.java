package io.gravitee.node.api.secrets.errors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretManagerConfigurationException extends RuntimeException {

    public SecretManagerConfigurationException(String message) {
        super(message);
    }

    public SecretManagerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretManagerConfigurationException(Throwable cause) {
        super(cause);
    }
}
