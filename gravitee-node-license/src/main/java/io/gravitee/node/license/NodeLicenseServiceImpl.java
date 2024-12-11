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

import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.Feature;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseModelService;
import io.gravitee.node.api.license.NodeLicenseService;
import io.gravitee.node.api.license.model.LicenseModel;
import io.gravitee.node.api.license.model.LicensePack;
import io.gravitee.node.api.license.model.LicenseTier;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class NodeLicenseServiceImpl implements NodeLicenseService {

    private static final String LICENSE_TIER_KEY = "tier";
    private static final String LICENSE_PACKS_KEY = "packs";
    private static final String LICENSE_FEATURES_KEY = "features";
    private static final String LEGACY_FEATURE_VALUE = "included";
    private static final String LIST_SEPARATOR = ",";

    private final Node node;

    private final LicenseModelService licenseModelService;

    private String tier;
    private Set<String> packs = Set.of();
    private Set<String> features = Set.of();

    public NodeLicenseServiceImpl(Node node, LicenseModelService licenseModelService) {
        this.node = node;
        this.licenseModelService = licenseModelService;
    }

    @Override
    public String getTier() {
        return tier;
    }

    @Override
    public Set<String> getPacks() {
        return packs;
    }

    @Override
    public Set<String> getFeatures() {
        return features;
    }

    @Override
    public boolean isFeatureEnabled(String featureName) {
        return featureName == null || features.contains(featureName);
    }

    @Override
    public void refresh() {
        tier = readTier();
        packs = readPacks();
        features = readFeatures();
    }

    private String readTier() {
        return readString(LICENSE_TIER_KEY).orElse(null);
    }

    private Set<String> readPacks() {
        Set<String> licensePacks = new HashSet<>(readList(LICENSE_PACKS_KEY));
        if (tier != null) {
            // Allow to declare a non existing tier. Useful when the license uses a tier that has been created for a newer version of gravitee-node
            LicenseTier licenseTier = licenseModelService.getLicenseModel().getTiers().get(tier);
            if (licenseTier != null) {
                Set<String> tierPacks = licenseTier.getPacks();
                licensePacks.addAll(tierPacks);
            } else if (log.isDebugEnabled()) {
                log.debug("Unknown tier: {}", tier);
            }
        }
        return licensePacks;
    }

    private Set<String> readFeatures() {
        HashSet<String> licenseFeatures = new HashSet<>();
        LicenseModel licenseModel = licenseModelService.getLicenseModel();
        licenseFeatures.addAll(readList(LICENSE_FEATURES_KEY));
        licenseFeatures.addAll(getLegacyFeatures());
        for (String pack : getPacks()) {
            // Allow to declare a non existing pack. Useful when the license uses a pack that has been created for a newer version of gravitee-node
            LicensePack licensePack = licenseModel.getPacks().get(pack);
            if (licensePack != null) {
                licenseFeatures.addAll(licensePack.getFeatures());
            } else if (log.isDebugEnabled()) {
                log.debug("Unknown pack: {}", pack);
            }
        }
        return licenseFeatures;
    }

    private Set<String> readList(String key) {
        return readString(key).map(value -> Set.of(value.split(LIST_SEPARATOR))).orElse(Set.of());
    }

    private Optional<String> readString(String featureKey) {
        return findLicense().flatMap(license -> license.feature(featureKey)).map(Feature::getString).filter(s -> !s.isBlank());
    }

    private Optional<License> findLicense() {
        return Optional.ofNullable(node.license());
    }

    private Set<String> getLegacyFeatures() {
        return findLicense().map(this::readLegacyFeatures).orElse(Set.of());
    }

    private Set<String> readLegacyFeatures(License license) {
        return license
            .features()
            .entrySet()
            .stream()
            .filter(entry -> LEGACY_FEATURE_VALUE.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(toSet());
    }
}
