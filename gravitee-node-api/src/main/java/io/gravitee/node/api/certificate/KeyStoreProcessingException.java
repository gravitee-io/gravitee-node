package io.gravitee.node.api.certificate;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeyStoreProcessingException extends RuntimeException {

    public KeyStoreProcessingException(String message) {
        super(message);
    }

    public KeyStoreProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
