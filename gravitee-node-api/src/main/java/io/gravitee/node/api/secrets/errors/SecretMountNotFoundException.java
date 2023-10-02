package io.gravitee.node.api.secrets.errors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretMountNotFoundException extends RuntimeException {

    public SecretMountNotFoundException(String message) {
        super(message);
    }

    public SecretMountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
