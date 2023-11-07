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
package io.gravitee.node.container.plugin;

import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.api.PluginDeploymentContext;
import lombok.AllArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class NodePluginDeploymentContext implements PluginDeploymentContext {

    private LicenseManager licenseManager;

    @Override
    public boolean isPluginDeployable(String featureName) {
        if (featureName != null) {
            final License platformLicense = licenseManager.getPlatformLicense();
            return platformLicense.isFeatureEnabled(featureName);
        }

        return true;
    }
}
