package io.gravitee.node.api.license;

import java.io.Serial;

/**
 * {@link ExpiredLicenseException} allows to identify that a license is expired.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 *
 * @see MalformedLicenseException
 */
public class ExpiredLicenseException extends InvalidLicenseException {

    @Serial
    private static final long serialVersionUID = 8229097167142139226L;

    public ExpiredLicenseException(String message) {
        super(message);
    }

    public ExpiredLicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
