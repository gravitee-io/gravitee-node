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

import io.gravitee.node.api.license.NodeLicenseService;
import io.gravitee.plugin.api.PluginDeploymentContext;
import io.gravitee.plugin.api.PluginDeploymentContextFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeDeploymentContextFactory implements PluginDeploymentContextFactory<PluginDeploymentContext> {

    private final NodeLicenseService nodeLicenseService;

    private NodePluginDeploymentContext nodePluginDeploymentContext;

    public NodeDeploymentContextFactory(NodeLicenseService nodeLicenseService) {
        this.nodeLicenseService = nodeLicenseService;
    }

    @Override
    public PluginDeploymentContext create() {
        if (nodePluginDeploymentContext == null) {
            nodePluginDeploymentContext = new NodePluginDeploymentContext(nodeLicenseService);
        }
        return nodePluginDeploymentContext;
    }
}
