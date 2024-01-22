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
package io.gravitee.node.container.spring;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.license.LicenseFactory;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.api.license.LicenseModelService;
import io.gravitee.node.container.plugin.NodeDeploymentContextFactory;
import io.gravitee.node.license.DefaultLicenseFactory;
import io.gravitee.node.license.DefaultLicenseManager;
import io.gravitee.node.license.DefaultLicenseModelService;
import io.gravitee.node.license.LicenseLoaderService;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.management.http.vertx.endpoint.DefaultManagementEndpointManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import org.springframework.context.annotation.Bean;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeContainerConfiguration {

    @Bean
    public NodeDeploymentContextFactory nodeDeploymentContextFactory(LicenseManager licenseManager) {
        return new NodeDeploymentContextFactory(licenseManager);
    }

    @Bean
    public LicenseManager licenseManager(PluginRegistry pluginRegistry) {
        return new DefaultLicenseManager(pluginRegistry);
    }

    @Bean
    public LicenseFactory managedLicenseFactory(LicenseModelService licenseModelService) {
        return new DefaultLicenseFactory(licenseModelService);
    }

    @Bean
    public LicenseModelService licenseModelService() {
        return new DefaultLicenseModelService();
    }

    @Bean
    public LicenseLoaderService licenseLoaderService(
        Configuration configuration,
        LicenseFactory licenseFactory,
        LicenseManager licenseManager,
        ManagementEndpointManager managementEndpointManager
    ) {
        return new LicenseLoaderService(configuration, licenseFactory, licenseManager, managementEndpointManager);
    }

    @Bean
    public ManagementEndpointManager managementEndpointManager() {
        return new DefaultManagementEndpointManager();
    }
}
