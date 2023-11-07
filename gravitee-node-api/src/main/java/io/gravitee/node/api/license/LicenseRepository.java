package io.gravitee.node.api.license;

import io.gravitee.node.api.license.model.License;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface LicenseRepository {
    /**
     * Return the {@link License} object corresponding to the specified organization.
     *
     * @param organizationId the organization identifier.
     * @return the {@link License} found or none if the specified organization has no license.
     */
    Maybe<License> findOrganizationLicense(String organizationId);

    /**
     * Return all the {@link License} corresponding to the specified criteria.
     *
     * @param criteria the criteria to match.
     * @return the list of {@link License} found.
     */
    Flowable<License> findByCriteria(LicenseCriteria criteria);

    /**
     * Create a license if it does not exist in database or update it if it's present (replace old values by new one).
     *
     * @param license the license to create or update.
     * @return the created or updated license.
     */
    Single<License> createOrUpdate(License license);
}
