package io.gravitee.node.license;

import static io.gravitee.node.license.license3j.License3J.LICENSE_EXPIRE_AT;

import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import io.gravitee.node.license.license3j.License3J;
import io.gravitee.node.license.license3j.License3JFeature;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@AllArgsConstructor
@EqualsAndHashCode
class DefaultLicense implements License {

    @Getter
    @Setter
    @Nonnull
    private String referenceType;

    @Getter
    @Setter
    @Nonnull
    private String referenceId;

    @Getter
    private String tier;

    @Getter
    @Builder.Default
    @Nonnull
    private Set<String> packs = Set.of();

    @Getter
    @Builder.Default
    private Set<String> features = Set.of();

    private License3J license3j;

    @Override
    public boolean isFeatureEnabled(String feature) {
        if (license3j.isValid() && !license3j.isExpired()) {
            return feature == null || features.contains(feature);
        }

        // If the underlying license is not valid or is expired then only plugins not requiring any feature are considered enabled.
        return feature == null;
    }

    @Override
    public void verify() throws InvalidLicenseException {
        this.license3j.verify();
    }

    @Override
    public Date getExpirationDate() {
        return this.license3j.feature(LICENSE_EXPIRE_AT).map(License3JFeature::getDate).orElse(null);
    }

    @Override
    public boolean isExpired() {
        final Date expirationDate = getExpirationDate();

        if (expirationDate != null) {
            return expirationDate.before(Date.from(Instant.now()));
        }
        return false;
    }

    @Override
    public @Nonnull Map<String, Object> getAttributes() {
        return license3j.features();
    }

    @Override
    public @Nonnull Map<String, String> getRawAttributes() {
        return license3j.featuresAsString();
    }
}
