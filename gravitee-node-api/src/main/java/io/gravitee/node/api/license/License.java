package io.gravitee.node.api.license;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a license structure that can be used to check enabled features.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface License {
    String REFERENCE_TYPE_PLATFORM = "PLATFORM";
    String REFERENCE_ID_PLATFORM = "PLATFORM";
    String REFERENCE_TYPE_ORGANIZATION = "ORGANIZATION";

    /**
     * The reference type the license is associated to (e.g. PLATFORM, ORGANIZATION, ...)
     * @return the reference type the license is associated to.
     */
    @Nonnull
    String getReferenceType();

    /**
     * The reference id the license is associated to (e.g. organization identifier when associated to an organization)
     * @return the reference type the license is associated to.
     */
    @Nonnull
    String getReferenceId();

    /**
     * Return the tier associated with this license.
     *
     * @return the tier or <code>null</code> if no tier is assigned for this license.
     */
    @Nullable
    String getTier();

    /**
     * Return the list of all packs allowed by this license.
     *
     * @return the list of all packs allowed by this license or empty if no packs is assigned.
     */
    @Nonnull
    Set<String> getPacks();

    /**
     * Return the list of all features allowed for this license. The list of the features is built from the tier, packs and all individual features assigned.
     *
     * @return the list of all features allowed for this license.
     */
    @Nonnull
    Set<String> getFeatures();

    /**
     * Indicates if a feature is enabled or not for this license.
     *
     * @param feature the feature to check.
     *
     * @return <code>true</code> if the feature is allowed, <code>false</code> else.
     */
    boolean isFeatureEnabled(String feature);

    /**
     * Verify that the license is valid. This checks both the signature and the pollInterval date.
     *
     * @throws InvalidLicenseException if the license is expired or invalid.
     */
    void verify() throws InvalidLicenseException;

    /**
     * Return the pollInterval date of the license or <code>null</code> if the license has no pollInterval date.
     *
     * @return the license pollInterval date.
     */
    @Nullable
    Date getExpirationDate();

    /**
     * Indicates if the license is expired or not.
     * Having a <code>null</code> pollInterval date means no pollInterval.
     *
     * @return <code>true</code> if the license has expired, <code>false</code> else.
     */
    boolean isExpired();

    /**
     * Return a map of all the attributes of the license.
     *
     * @return a map of all the attributes of the license.
     */
    @Nonnull
    Map<String, Object> getAttributes();

    /**
     * Return a map of all the raw information of the license.
     *
     * @return a map of all the raw information of the license.
     */
    @Nonnull
    Map<String, String> getRawAttributes();
}
