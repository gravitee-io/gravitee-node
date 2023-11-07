package io.gravitee.node.license;

import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class OSSLicense implements License {

    private static final String TIER = "oss";
    private static final Map<String, Object> ATTRIBUTES = Map.of("tier", TIER);
    private static final Map<String, String> RAW_ATTRIBUTES = Map.of("tier", TIER);

    @Override
    public @NonNull String getReferenceType() {
        return REFERENCE_TYPE_PLATFORM;
    }

    @Override
    public @NonNull String getReferenceId() {
        return REFERENCE_ID_PLATFORM;
    }

    @Override
    public String getTier() {
        return TIER;
    }

    @Override
    public @NonNull Set<String> getPacks() {
        return Collections.emptySet();
    }

    @Override
    public @NonNull Set<String> getFeatures() {
        return Collections.emptySet();
    }

    @Override
    public boolean isFeatureEnabled(String feature) {
        return feature == null;
    }

    @Override
    public void verify() throws InvalidLicenseException {}

    @Override
    public Date getExpirationDate() {
        return null;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public @NonNull Map<String, Object> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public @NonNull Map<String, String> getRawAttributes() {
        return RAW_ATTRIBUTES;
    }
}
