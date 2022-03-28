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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler is responsible to listen to all produced monitoring events and persist them.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClusteredNodeMonitoringEventHandler extends NodeMonitoringEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusteredNodeMonitoringEventHandler.class);

    private final ClusterManager clusterManager;
    private final HazelcastInstance hazelcastInstance;

    protected ITopic<HealthCheck> distributedHealthCheckTopic;
    protected ITopic<Monitor> distributedMonitorTopic;
    protected ITopic<NodeInfos> distributedNodeInfosTopic;
    private UUID healthCheckTopicListenerId;
    private UUID monitorTopicListenerId;
    private UUID nodeInfosTopicListenerId;

    public ClusteredNodeMonitoringEventHandler(
        Vertx vertx,
        ObjectMapper objectMapper,
        Node node,
        NodeMonitoringService nodeMonitoringService,
        ClusterManager clusterManager,
        HazelcastInstance hazelcastInstance
    ) {
        super(vertx, objectMapper, node, nodeMonitoringService);
        this.clusterManager = clusterManager;
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        prepareListeners();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        removeListeners();
    }

    private void prepareListeners() {
        this.distributedHealthCheckTopic = hazelcastInstance.getReliableTopic("node-healthcheck");
        this.distributedMonitorTopic = hazelcastInstance.getReliableTopic("node-monitor");
        this.distributedNodeInfosTopic = hazelcastInstance.getReliableTopic("node-infos");

        this.healthCheckTopicListenerId =
            distributedHealthCheckTopic.addMessageListener(message -> {
                if (clusterManager.isMasterNode()) {
                    LOGGER.debug("Received health check message from distributed topic");
                    handleHealthCheck(message.getMessageObject());
                }
            });

        this.monitorTopicListenerId =
            distributedMonitorTopic.addMessageListener(message -> {
                if (clusterManager.isMasterNode()) {
                    LOGGER.debug("Received monitor message from distributed topic");
                    handleMonitor(message.getMessageObject());
                }
            });

        this.nodeInfosTopicListenerId =
            distributedNodeInfosTopic.addMessageListener(message -> {
                if (clusterManager.isMasterNode()) {
                    LOGGER.debug("Received node infos message from distributed topic");
                    handleNodeInfos(message.getMessageObject());
                }
            });
    }

    private void removeListeners() {
        this.distributedHealthCheckTopic.removeMessageListener(this.healthCheckTopicListenerId);
        this.distributedMonitorTopic.removeMessageListener(this.monitorTopicListenerId);
        this.distributedNodeInfosTopic.removeMessageListener(this.nodeInfosTopicListenerId);
    }

    @Override
    protected void handleHealthCheckMessage(Message<HealthCheck> message) {
        LOGGER.debug("Received health check message from internal bus");
        // We are in a cluster and distributed node monitoring is enabled. Propagate monitoring data across the cluster, it will be handled by the master node.
        distributedHealthCheckTopic.publish(message.body());
    }

    @Override
    protected void handleMonitorMessage(Message<Monitor> message) {
        LOGGER.debug("Received monitor message from internal bus");
        distributedMonitorTopic.publish(message.body());
    }

    @Override
    protected void handleNodeInfosMessage(Message<NodeInfos> message) {
        LOGGER.debug("Received health check message from internal bus");
        distributedNodeInfosTopic.publish(message.body());
    }
}
