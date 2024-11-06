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
import io.gravitee.node.api.healthcheck.ProbeEvaluator;
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.monitoring.DefaultNodeMonitoringService;
import io.gravitee.node.monitoring.DefaultProbeEvaluator;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.handler.NodeMonitoringEventHandler;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckManagementEndpoint;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.healthcheck.ProbeManagerImpl;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorManagementEndpoint;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.vertx.core.Vertx;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;

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
    public NodeMonitorService nodeMonitorService(
        NodeMonitorManagementEndpoint nodeMonitorManagementEndpoint,
        ManagementEndpointManager managementEndpointManager,
        AlertEventProducer alertEventProducer,
        Node node,
        Vertx vertx,
        MonitoringConfiguration monitoringConfiguration
    ) {
        return new NodeMonitorService(
            nodeMonitorManagementEndpoint,
            managementEndpointManager,
            alertEventProducer,
            node,
            vertx,
            monitoringConfiguration
        );
    }

    @Bean
    public NodeMonitorManagementEndpoint nodeMonitorManagementEndpoint(ObjectMapper objectMapper) {
        return new NodeMonitorManagementEndpoint(objectMapper);
    }

    @Bean
    public HealthConfiguration healthConfiguration(
        @Value("${services.health.enabled:true}") boolean enabled,
        @Value("${services.health.delay:5000}") int delay,
        @Value("${services.health.unit:MILLISECONDS}") TimeUnit unit,
        @Value("${services.health.threshold.cpu:80}") int cpuThreshold,
        @Value("${services.health.threshold.memory:80}") int memoryThreshold
    ) {
        return new HealthConfiguration(enabled, delay, unit, cpuThreshold, memoryThreshold);
    }

    @Bean
    public MonitoringConfiguration monitoringConfiguration(
        @Value("${services.monitoring.enabled:true}") boolean enabled,
        @Value("${services.monitoring.delay:5000}") int delay,
        @Value("${services.monitoring.unit:MILLISECONDS}") TimeUnit unit
    ) {
        return new MonitoringConfiguration(enabled, delay, unit);
    }

    @Bean
    public ProbeEvaluator probeRegistry(ProbeManager probeManager, HealthConfiguration healthConfiguration) {
        return new DefaultProbeEvaluator(healthConfiguration.unit().toMillis(healthConfiguration.delay()), probeManager);
    }

    @Bean
    public NodeHealthCheckService nodeHealthCheckService(
        ManagementEndpointManager managementEndpointManager,
        DefaultProbeEvaluator probeRegistry,
        NodeHealthCheckManagementEndpoint healthCheckEndpoint,
        AlertEventProducer alertEventProducer,
        Node node,
        Vertx vertx,
        HealthConfiguration healthConfiguration
    ) {
        return new NodeHealthCheckService(
            managementEndpointManager,
            probeRegistry,
            healthCheckEndpoint,
            alertEventProducer,
            node,
            vertx,
            healthConfiguration
        );
    }

    @Bean
    public NodeHealthCheckManagementEndpoint nodeHealthCheckManagementEndpoint(ProbeEvaluator probeEvaluator, ObjectMapper objectMapper) {
        return new NodeHealthCheckManagementEndpoint(probeEvaluator, objectMapper);
    }

    @Bean
    public NodeInfosService nodeInfosService(PluginRegistry pluginRegistry, ConfigurableEnvironment environment, Node node, Vertx vertx) {
        return new NodeInfosService(pluginRegistry, environment, node, vertx);
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
