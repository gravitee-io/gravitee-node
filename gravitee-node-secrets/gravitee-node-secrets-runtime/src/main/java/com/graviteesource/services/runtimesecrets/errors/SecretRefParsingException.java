package com.graviteesource.services.runtimesecrets.errors;

import io.gravitee.node.api.secrets.runtime.RuntimeSecretException;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretRefParsingException extends RuntimeSecretException {

    public SecretRefParsingException(String message) {
        super(message);
    }
}
