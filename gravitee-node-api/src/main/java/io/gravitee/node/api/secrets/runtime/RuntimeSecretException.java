package io.gravitee.node.api.secrets.runtime;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RuntimeSecretException extends RuntimeException {

    public RuntimeSecretException() {}

    public RuntimeSecretException(String message) {
        super(message);
    }

    public RuntimeSecretException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeSecretException(Throwable cause) {
        super(cause);
    }
}
