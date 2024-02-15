package io.gravitee.node.api.license;

import java.io.Serial;

/**
 * {@link InvalidLicenseException} allows to identify that a license has an invalid signature or is expired.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 *
 * @see MalformedLicenseException
 */
public class InvalidLicenseException extends Exception {

    @Serial
    private static final long serialVersionUID = -1669115414217408129L;

    public InvalidLicenseException(String message) {
        super(message);
    }

    public InvalidLicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
