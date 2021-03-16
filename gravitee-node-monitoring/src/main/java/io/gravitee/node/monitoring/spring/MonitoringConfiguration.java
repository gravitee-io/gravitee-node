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
package io.gravitee.node.monitoring.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.handler.ClusteredNodeMonitoringEventHandler;
import io.gravitee.node.monitoring.handler.NodeMonitoringEventHandler;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckManagementEndpoint;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.healthcheck.ProbeManagerImpl;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorManagementEndpoint;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author Jeoffre HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(NodeMonitoringService.class)
public class MonitoringConfiguration {

    @Value("${services.monitoring.distributed:false}")
    protected boolean distributed;

    @Bean
    public NodeMonitorService nodeMonitorService() {
        return new NodeMonitorService();
    }

    @Bean
    public NodeMonitorManagementEndpoint nodeMonitorManagementEndpoint() {
        return new NodeMonitorManagementEndpoint();
    }

    @Bean
    public NodeHealthCheckService nodeHealthCheckService() {
        return new NodeHealthCheckService();
    }

    @Bean
    public NodeHealthCheckManagementEndpoint nodeHealthCheckManagementEndpoint() {
        return new NodeHealthCheckManagementEndpoint();
    }

    @Bean
    public NodeInfosService nodeInfosService() {
        return new NodeInfosService();
    }

    @Bean
    public NodeMonitoringEventHandler nodeMonitoringEventHandler(Vertx vertx, ApplicationContext context, ObjectMapper objectMapper, Node node, NodeMonitoringService nodeMonitoringService) {

        // Instantiate the monitoring event handler depending on the clustering mode.
        NodeMonitoringEventHandler nodeMonitoringEventHandler = null;

        if (distributed) {
            try {
                // Try to retrieve cluster beans.
                final ClusterManager clusterManager = context.getBean(ClusterManager.class);
                final HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
                nodeMonitoringEventHandler = new ClusteredNodeMonitoringEventHandler(vertx, objectMapper, node, nodeMonitoringService, clusterManager, hazelcastInstance);
            } catch (NoClassDefFoundError | NoSuchBeanDefinitionException e) {
                // There is no clustering on that node.
            }
        }

        if (nodeMonitoringEventHandler == null) {
            nodeMonitoringEventHandler = new NodeMonitoringEventHandler(vertx, objectMapper, node, nodeMonitoringService);
        }

        return nodeMonitoringEventHandler;
    }

    @Bean
    public ProbeManager probeManager() {
        return new ProbeManagerImpl();
    }
}
