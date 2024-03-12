package io.gravitee.node.api.license;

import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Interface to implement a way to 'fetch' a license.
 * Concrete implementations could be from local file, from remote http location or anything else that is relevant.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface LicenseFetcher {
    /**
     * Fetch the license.
     * In case there is no license to fetch it may return <code>null</code>.
     *
     * @return the {@link License} fetched or <code>null</code> if no license has been fetched.
     * @throws InvalidLicenseException thrown if the specified license data is invalid.
     * @throws MalformedLicenseException thrown if the license is malformed.
     */
    @Nullable
    License fetch() throws InvalidLicenseException, MalformedLicenseException;

    /**
     * Start the watching of the license to detect changes.
     * The <code>onChange</code> callback will be called with the update {@link License} in case a change is detected.
     *
     * @param onChange the callback action that will be called in case a change on the license is detected.
     */
    void startWatch(Consumer<License> onChange);

    /**
     * Stop the watching of the license.
     */
    void stopWatch();
}
