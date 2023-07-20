package io.gravitee.node.secrets.api.errors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretManagerException extends RuntimeException {

    public SecretManagerException(String message) {
        super(message);
    }

    public SecretManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretManagerException(Throwable cause) {
        super(cause);
    }
}
