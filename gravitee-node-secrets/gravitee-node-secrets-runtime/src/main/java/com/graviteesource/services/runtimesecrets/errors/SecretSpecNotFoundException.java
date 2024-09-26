package com.graviteesource.services.runtimesecrets.errors;

import io.gravitee.node.api.secrets.runtime.RuntimeSecretException;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretSpecNotFoundException extends RuntimeSecretException {

    public SecretSpecNotFoundException(String message) {
        super(message);
    }
}
