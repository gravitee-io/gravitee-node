package io.gravitee.node.secrets.plugin.mock;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MockSecretProviderException extends RuntimeException {

    public MockSecretProviderException(String message) {
        super(message);
    }
}
