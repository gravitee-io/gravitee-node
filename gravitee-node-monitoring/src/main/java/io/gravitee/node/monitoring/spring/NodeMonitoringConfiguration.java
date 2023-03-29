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
import io.gravitee.node.api.Node;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.gravitee.node.monitoring.DefaultNodeMonitoringService;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.handler.NodeMonitoringEventHandler;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckManagementEndpoint;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.healthcheck.ProbeManagerImpl;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorManagementEndpoint;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * @author Jeoffre HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMonitoringConfiguration {

    @Bean
    public NodeMonitoringService nodeMonitoringService(@Lazy NodeMonitoringRepository repository) {
        return new DefaultNodeMonitoringService(repository);
    }

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
    public NodeMonitoringEventHandler nodeMonitoringEventHandler(
        Vertx vertx,
        @Lazy ClusterManager clusterManager,
        ObjectMapper objectMapper,
        Node node,
        DefaultNodeMonitoringService nodeMonitoringService
    ) {
        return new NodeMonitoringEventHandler(vertx, clusterManager, objectMapper, node, nodeMonitoringService);
    }

    @Bean
    public ProbeManager probeManager() {
        return new ProbeManagerImpl();
    }
}
