package io.gravitee.node.license;

import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
class OSSLicense implements License {

    static final String TIER = "oss";
    private static final Map<String, Object> ATTRIBUTES = Map.of("tier", TIER);
    private static final Map<String, String> RAW_ATTRIBUTES = Map.of("tier", TIER);

    private String referenceType = REFERENCE_TYPE_PLATFORM;
    private String referenceId = REFERENCE_ID_PLATFORM;

    @Override
    public @NonNull String getReferenceType() {
        return referenceType;
    }

    @Override
    public @NonNull String getReferenceId() {
        return referenceId;
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
