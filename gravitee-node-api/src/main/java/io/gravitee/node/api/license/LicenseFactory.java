package io.gravitee.node.api.license;

import javax.annotation.Nonnull;

/**
 * Factory allowing to create a {@link License} from binary or base64 string.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface LicenseFactory {
    /**
     * Create a {@link License} from a base64 encoded string.
     *
     * @param referenceType the reference type the license is created for (e.g. PLATFORM, ORGANIZATION, ...).
     * @param referenceId the reference id the license is created for.
     * @param base64License a base64 encoded string representing the license data.
     *
     * @return the created {@link License}. An OSS license is returned if base64License is null.
     * @throws InvalidLicenseException thrown if the specified license data is invalid.
     * @throws MalformedLicenseException thrown if the license is malformed.
     */
    License create(@Nonnull String referenceType, @Nonnull String referenceId, String base64License)
        throws InvalidLicenseException, MalformedLicenseException;

    /**
     * Create a {@link License} from a binary content.
     *
     * @param referenceType the reference type the license is created for (e.g. PLATFORM, ORGANIZATION, ...).
     * @param referenceId the reference id the license is created for.
     *
     * @param bytesLicense a byte array representing the license data.
     * @return the created {@link License}. An OSS license is returned if bytesLicense is null.
     * @throws InvalidLicenseException thrown if the specified license data is invalid.

     * @throws MalformedLicenseException thrown if the license is malformed.
     */
    License create(@Nonnull String referenceType, @Nonnull String referenceId, byte[] bytesLicense)
        throws InvalidLicenseException, MalformedLicenseException;
}
