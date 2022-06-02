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
package io.gravitee.node.api.deployer;

import io.gravitee.node.api.license.License;
import io.gravitee.node.api.plugin.NodeDeploymentContext;
import io.gravitee.plugin.api.PluginDeploymentLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPluginDeploymentLifecycle implements PluginDeploymentLifecycle<NodeDeploymentContext> {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected abstract String getFeatureName();

    @Override
    public boolean isDeployable(NodeDeploymentContext context) {
        License license = context.node().license();

        boolean deployable = false;
        if (license != null) {
            deployable = license.isFeatureIncluded(getFeatureName());
        }

        if (!deployable) {
            logger.warn("Feature {} is missing from the license", getFeatureName());
        }

        return deployable;
    }
}
