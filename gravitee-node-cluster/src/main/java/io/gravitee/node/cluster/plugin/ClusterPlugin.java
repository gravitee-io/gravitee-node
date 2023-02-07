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
package io.gravitee.node.cluster.plugin;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.plugin.core.api.Plugin;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ClusterPlugin extends Plugin {
    String PLUGIN_TYPE = "cluster";

    Class<? extends ClusterManager> clusterManager();

    @Override
    default String type() {
        return PLUGIN_TYPE;
    }
}
