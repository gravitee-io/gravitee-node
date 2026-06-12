/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.license;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.license.model.LicenseModel;
import io.gravitee.node.api.license.model.LicenseTier;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class DefaultLicenseModelServiceTest {

    private final DefaultLicenseModelService cut = new DefaultLicenseModelService();

    @Test
    void should_contain_expected_planet_tier_packs() {
        LicenseTier tier = getTier("planet");

        assertThat(tier).isNotNull();
        assertThat(tier.getPacks())
            .containsExactlyInAnyOrder("enterprise-features", "enterprise-legacy-upgrade", "enterprise-identity-provider");
    }

    @Test
    void should_contain_expected_galaxy_tier_packs() {
        LicenseTier tier = getTier("galaxy");

        assertThat(tier).isNotNull();
        assertThat(tier.getPacks())
            .containsExactlyInAnyOrder(
                "enterprise-features",
                "enterprise-legacy-upgrade",
                "enterprise-identity-provider",
                "observability",
                "enterprise-policy",
                "enterprise-alert-engine"
            );
    }

    @Test
    void should_contain_expected_universe_tier_packs() {
        LicenseTier tier = getTier("universe");

        assertThat(tier).isNotNull();
        assertThat(tier.getPacks())
            .containsExactlyInAnyOrder(
                "enterprise-features",
                "enterprise-legacy-upgrade",
                "enterprise-identity-provider",
                "observability",
                "enterprise-policy",
                "event-native",
                "enterprise-mfa-factor",
                "enterprise-secret-manager",
                "enterprise-alert-engine",
                "enterprise-authenticator"
            );
    }

    private LicenseTier getTier(String tierName) {
        LicenseModel model = cut.getLicenseModel();
        return model.getTiers().get(tierName);
    }
}
