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
package io.gravitee.node.cluster;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.cluster.endpoint.ClusterEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import lombok.CustomLog;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class NodeClusterService extends AbstractService<NodeClusterService> {

    @Autowired
    private Node node;

    @Autowired
    @Lazy
    private ClusterManager clusterManager;

    @Autowired
    private ManagementEndpointManager managementEndpointManager;

    @Override
    public void doStart() throws Exception {
        super.doStart();
        node.metadata().put("node.id", node.id());
        node.metadata().put("node.hostname", node.hostname());
        try {
            clusterManager.start();
            managementEndpointManager.register(new ClusterEndpoint(clusterManager));
        } catch (NoSuchBeanDefinitionException e) {
            log.error("No Cluster manager has been registered.");
            throw new NoClusterManagerException();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (clusterManager != null) {
            clusterManager.stop();
        }
    }
}
