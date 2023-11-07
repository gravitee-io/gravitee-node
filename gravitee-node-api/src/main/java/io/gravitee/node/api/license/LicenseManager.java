package io.gravitee.node.api.license;

import io.gravitee.common.service.Service;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.NonNull;

/**
 * Manager allowing getting access to the licence and supports platform or organization licenses.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface LicenseManager extends Service<LicenseManager> {
    /**
     * Register a license for the specified organization.
     * In case a license is already associated to the organization, the new license will replace it.
     *
     * @param organizationId the organization identifier.
     * @param license the license to associate to the specified organization.
     *
     * @see #getOrganizationLicense(String)
     * @see #getOrganizationLicenseOrPlatform(String)
     */
    void registerOrganizationLicense(@NonNull String organizationId, License license);

    /**
     * Register the specified license as the platform license.
     *
     * @param license the platform license.
     *
     * @see #getPlatformLicense()
     * @see #getOrganizationLicenseOrPlatform(String)
     */
    void registerPlatformLicense(License license);

    /**
     * Get the organization license or <code>null</code> if there is no license associated to this organization.
     *
     * @param organizationId the organization identifier.
     * @return the organization license or <code>null</code> if there is no license associated to this organization.
     *
     * @see #getOrganizationLicenseOrPlatform(String)
     */
    @Nullable
    License getOrganizationLicense(String organizationId);

    /**
     * Get the organization license or the platform license if there is no license associated to this organization.
     *
     * @param organizationId the organization identifier.
     * @return the organization license or the platform license if there is no license associated to this organization.
     *
     * @see #getPlatformLicense()
     */
    @NonNull
    License getOrganizationLicenseOrPlatform(String organizationId);

    /**
     * Get the platform license. In case no license has been loaded an OSS license with no feature will be returned.
     *
     * @return the platform license.
     */
    @NonNull
    License getPlatformLicense();

    /**
     * Validate the usage of the specified plugins is allowed by the license for the specified organization.
     * For each specified plugin, the corresponding feature will be extracted and compared to the features allowed by the license.
     * The organization license will be used with a fallback to the platform license.
     *
     * @param organizationId the organization for which to validate the plugin usage.
     * @param plugins the list of plugin to validate.
     *
     * @throws InvalidLicenseException thrown in case the license has expired.
     * @throws ForbiddenFeatureException thrown in case the license does not allow a feature.
     */
    void validatePluginFeatures(String organizationId, Collection<Plugin> plugins)
        throws InvalidLicenseException, ForbiddenFeatureException;

    /**
     * Register a listener that will be called each time a license expires.
     *
     * @param expirationListener the listener that will be called each time a license expires.
     */
    void onLicenseExpires(Consumer<License> expirationListener);

    /**
     * Plugin information for feature validation purpose.
     *
     * @param type the type of the plugin.
     * @param id the id of the plugin.
     */
    record Plugin(String type, String id) {}

    /**
     * Information about a feature forbidden by the license.
     *
     * @param feature the feature name.
     * @param plugin the plugin that requires this feature.
     */
    record ForbiddenFeature(String feature, String plugin) {}
}
