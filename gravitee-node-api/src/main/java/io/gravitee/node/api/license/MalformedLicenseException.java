package io.gravitee.node.api.license;

import java.io.Serial;

/**
 * {@link MalformedLicenseException} allows to detect unreadable license content.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 *
 * @see InvalidLicenseException
 */
public class MalformedLicenseException extends Exception {

    @Serial
    private static final long serialVersionUID = -77510984950331826L;

    public MalformedLicenseException(String message) {
        super(message);
    }

    public MalformedLicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
