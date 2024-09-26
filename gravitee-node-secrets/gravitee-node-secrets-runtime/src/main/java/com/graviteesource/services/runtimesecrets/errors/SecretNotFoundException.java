package com.graviteesource.services.runtimesecrets.errors;

import io.gravitee.node.api.secrets.runtime.RuntimeSecretException;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretNotFoundException extends RuntimeSecretException {

    public SecretNotFoundException(String message) {
        super(message);
    }
}
