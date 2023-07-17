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
import io.gravitee.node.api.license.LicenseModelService;
import io.gravitee.node.api.license.NodeLicenseService;
import io.gravitee.node.container.plugin.NodeDeploymentContextFactory;
import io.gravitee.node.license.LicenseModelServiceImpl;
import io.gravitee.node.license.LicenseService;
import io.gravitee.node.license.NodeLicenseServiceImpl;
import org.springframework.context.annotation.Bean;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeContainerConfiguration {

    @Bean
    public NodeDeploymentContextFactory nodeDeploymentContextFactory(NodeLicenseService nodeLicenseService) {
        return new NodeDeploymentContextFactory(nodeLicenseService);
    }

    @Bean
    public LicenseService licenseService() {
        return new LicenseService();
    }

    @Bean
    public LicenseModelService licenseModelService() {
        return new LicenseModelServiceImpl();
    }

    @Bean
    public NodeLicenseService nodeLicenseService(Node node, LicenseModelService licenseModelService) {
        return new NodeLicenseServiceImpl(node, licenseModelService);
    }
}
