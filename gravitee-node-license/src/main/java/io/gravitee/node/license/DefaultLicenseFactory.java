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

import static java.util.stream.Collectors.toSet;

import io.gravitee.node.api.license.*;
import io.gravitee.node.api.license.model.LicenseModel;
import io.gravitee.node.license.license3j.License3J;
import io.gravitee.node.license.license3j.License3JFeature;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.annotation.Nonnull;
import javax0.license3j.io.LicenseReader;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultLicenseFactory implements LicenseFactory {

    private static final String LICENSE_TIER_KEY = "tier";
    private static final String LICENSE_PACKS_KEY = "packs";
    private static final String LICENSE_FEATURES_KEY = "features";
    private static final String LEGACY_FEATURE_VALUE = "included";
    private static final String LIST_SEPARATOR = ",";

    private final LicenseModelService licenseModelService;

    public DefaultLicenseFactory(LicenseModelService licenseModelService) {
        this.licenseModelService = licenseModelService;
    }

    @Override
    public License create(@Nonnull String referenceType, @Nonnull String referenceId, @Nonnull String base64License)
        throws InvalidLicenseException, MalformedLicenseException {
        return create(referenceType, referenceId, Base64.getDecoder().decode(base64License));
    }

    @Override
    public License create(@Nonnull String referenceType, @Nonnull String referenceId, @Nonnull byte[] bytesLicense)
        throws InvalidLicenseException, MalformedLicenseException {
        try (LicenseReader reader = new LicenseReader(new ByteArrayInputStream(bytesLicense))) {
            return create(referenceType, referenceId, new License3J(reader.read()));
        } catch (InvalidLicenseException lie) {
            throw lie;
        } catch (Exception e) {
            throw new MalformedLicenseException("License cannot be read", e);
        }
    }

    @Override
    public License createOSSOrganization(@Nonnull String referenceId) {
        return new OSSLicense(referenceId, License.REFERENCE_TYPE_ORGANIZATION);
    }

    private License create(String referenceType, String referenceId, License3J license) throws InvalidLicenseException {
        // First, verify the license is valid.
        license.verify();

        final String tier = readTier(license);
        final Set<String> packs = readPacks(license, tier);
        final Set<String> features = readFeatures(license, packs);

        return DefaultLicense
            .builder()
            .referenceType(referenceType)
            .referenceId(referenceType)
            .tier(tier)
            .packs(packs)
            .features(features)
            .license3j(license)
            .build();
    }

    private String readTier(License3J license) {
        return readString(license, LICENSE_TIER_KEY).orElse(null);
    }

    private Set<String> readPacks(License3J license, String tier) {
        final Set<String> licensePacks = new HashSet<>(readList(license, LICENSE_PACKS_KEY));

        if (tier != null) {
            Set<String> tierPacks = licenseModelService.getLicenseModel().getTiers().get(tier).getPacks();
            licensePacks.addAll(tierPacks);
        }

        return licensePacks;
    }

    private Set<String> readFeatures(License3J license, Set<String> packs) {
        final HashSet<String> licenseFeatures = new HashSet<>();
        final LicenseModel licenseModel = licenseModelService.getLicenseModel();

        licenseFeatures.addAll(readList(license, LICENSE_FEATURES_KEY));
        licenseFeatures.addAll(readLegacyFeatures(license));

        for (String pack : packs) {
            licenseFeatures.addAll(licenseModel.getPacks().get(pack).getFeatures());
        }

        return licenseFeatures;
    }

    private Set<String> readList(License3J license, String key) {
        return readString(license, key).map(value -> Set.of(value.split(LIST_SEPARATOR))).orElse(Set.of());
    }

    private Optional<String> readString(License3J license, String featureKey) {
        return license.feature(featureKey).map(License3JFeature::getString).filter(s -> !s.isBlank());
    }

    private Set<String> readLegacyFeatures(License3J license) {
        return license
            .features()
            .entrySet()
            .stream()
            .filter(entry -> LEGACY_FEATURE_VALUE.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(toSet());
    }
}
