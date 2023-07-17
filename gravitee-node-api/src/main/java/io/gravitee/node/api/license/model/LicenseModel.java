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
package io.gravitee.node.api.license.model;

import java.util.Map;
import lombok.Data;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class LicenseModel {

    private Map<String, LicensePack> packs;
    private Map<String, LicenseTier> tiers;

    public boolean isGraviteeTier(String tierName) {
        return tiers.containsKey(tierName);
    }

    public boolean isGraviteePack(String packName) {
        return packs.keySet().stream().anyMatch(pack -> pack.equals(packName));
    }

    public boolean isGraviteeFeature(String featureName) {
        return packs.values().stream().anyMatch(pack -> pack.getFeatures().contains(featureName));
    }
}
