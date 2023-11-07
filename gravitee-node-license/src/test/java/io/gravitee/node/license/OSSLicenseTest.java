package io.gravitee.node.license;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class OSSLicenseTest {

    private final OSSLicense cut = new OSSLicense();

    @Test
    void should_return_oss_tier() {
        assertThat(cut.getTier()).isEqualTo("oss");
        assertThat(cut.getPacks()).isEmpty();
        assertThat(cut.getFeatures()).isEmpty();
        assertThat(cut.getFeatures()).isEmpty();
        assertThat(cut.getExpirationDate()).isNull();
        assertThat(cut.getAttributes()).containsEntry("tier", "oss");
        assertThat(cut.getRawAttributes()).containsEntry("tier", "oss");
    }

    @Test
    void should_allow_feature_when_feature_is_null() {
        assertThat(cut.isFeatureEnabled(null)).isTrue();
    }

    @Test
    void should_disallow_feature_when_feature_is_not_null() {
        assertThat(cut.isFeatureEnabled("something")).isFalse();
    }
}
