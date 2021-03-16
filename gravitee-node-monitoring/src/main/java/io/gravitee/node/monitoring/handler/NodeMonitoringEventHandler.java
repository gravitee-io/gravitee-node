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
package io.gravitee.node.monitoring.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This handler is responsible to listen to all produced monitoring events and persist them.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMonitoringEventHandler extends AbstractService<NodeMonitoringEventHandler> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeMonitoringEventHandler.class);

    protected final Vertx vertx;
    protected final ObjectMapper objectMapper;
    protected final Node node;
    protected final NodeMonitoringService nodeMonitoringService;

    public NodeMonitoringEventHandler(Vertx vertx, ObjectMapper objectMapper, Node node, NodeMonitoringService nodeMonitoringService) {
        this.vertx = vertx;
        this.objectMapper = objectMapper;
        this.node = node;
        this.nodeMonitoringService = nodeMonitoringService;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        vertx.eventBus().localConsumer(NodeInfosService.GIO_NODE_INFOS_BUS, this::handleNodeInfosMessage);
        vertx.eventBus().localConsumer(NodeHealthCheckService.GIO_NODE_HEALTHCHECK_BUS, this::handleHealthCheckMessage);
        vertx.eventBus().localConsumer(NodeMonitorService.GIO_NODE_MONITOR_BUS, this::handleMonitorMessage);
    }

    protected void handleNodeInfosMessage(Message<NodeInfos> message) {
        LOGGER.debug("Received node infos message from internal bus");
        handleNodeInfos(message.body());
    }

    protected void handleHealthCheckMessage(Message<HealthCheck> message) {
        LOGGER.debug("Received health check message from internal bus");
        handleHealthCheck(message.body());
    }

    protected void handleMonitorMessage(Message<Monitor> message) {
        LOGGER.debug("Received monitor message from internal bus");
        handleMonitor(message.body());
    }

    protected void handleNodeInfos(NodeInfos nodeInfos) {
        LOGGER.debug("Received node infos message from internal bus");
        nodeMonitoringService.createOrUpdate(convert(nodeInfos)).subscribe();
    }

    protected void handleHealthCheck(HealthCheck healthCheck) {
        LOGGER.debug("Processing health check data");
        nodeMonitoringService.createOrUpdate(convert(healthCheck)).subscribe();
    }

    protected void handleMonitor(Monitor monitor) {
        LOGGER.debug("Processing monitor data");
        nodeMonitoringService.createOrUpdate(convert(monitor)).subscribe();
    }

    private Monitoring convert(NodeInfos nodeInfos) {
      final Monitoring monitoring = buildMonitoring(nodeInfos);
      monitoring.setEvaluatedAt(new Date(nodeInfos.getEvaluatedAt()));
      monitoring.setType(Monitoring.NODE_INFOS);

      return monitoring;
    }


    private Monitoring convert(HealthCheck healthCheck) {
        final Monitoring monitoring = buildMonitoring(healthCheck);
        monitoring.setEvaluatedAt(new Date(healthCheck.getEvaluatedAt()));
        monitoring.setType(Monitoring.HEALTH_CHECK);

        return monitoring;
    }

    private Monitoring convert(Monitor monitor) {
        final Monitoring monitoring = buildMonitoring(monitor);
        monitoring.setEvaluatedAt(new Date(monitor.getTimestamp()));
        monitoring.setType(Monitoring.MONITOR);

        return monitoring;
    }

    private Monitoring buildMonitoring(Object payload) {

        final Monitoring monitoring = new Monitoring();
        monitoring.setNodeId(node.id());

        try {
            monitoring.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            LOGGER.error("An error occurred when trying to serialize monitoring payload to json");
        }

        return monitoring;
    }
}