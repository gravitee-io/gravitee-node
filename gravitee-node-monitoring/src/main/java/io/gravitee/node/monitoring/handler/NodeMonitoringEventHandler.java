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
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This handler is responsible to listen to all produced monitoring events and persist them.
 * Persistence is done only on primary node.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class NodeMonitoringEventHandler extends AbstractService<NodeMonitoringEventHandler> {

    private final Vertx vertx;
    private final ClusterManager clusterManager;
    private final ObjectMapper objectMapper;
    private final Node node;
    private final NodeMonitoringService nodeMonitoringService;
    private Topic<NodeInfos> nodeInfosTopic;
    private Topic<HealthCheck> healthCheckTopic;
    private Topic<Monitor> monitorTopic;
    private String monitorSubscriptionId;
    private String healthCheckSubscription;
    private String nodeInfoSubscription;
    private MessageConsumer<NodeInfos> nodeInfosMessageConsumer;
    private MessageConsumer<HealthCheck> healthCheckMessageConsumer;
    private MessageConsumer<Monitor> monitorMessageConsumer;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        registerClusterListener();
        registerInternalListener();
    }

    private void registerClusterListener() {
        nodeInfosTopic = clusterManager.topic("node-infos");
        nodeInfoSubscription =
            nodeInfosTopic.addMessageListener(message -> {
                log.debug("Received node infos message from cluster");
                if (clusterManager.self().primary()) {
                    log.debug("Processing node infos message");
                    nodeMonitoringService
                        .createOrUpdate(convert(message.content()))
                        .ignoreElement()
                        .onErrorResumeNext(throwable -> {
                            log.error("Unable to process node infos message", throwable);
                            return Completable.complete();
                        })
                        .subscribe();
                }
            });
        healthCheckTopic = clusterManager.topic("node-healthcheck");
        healthCheckSubscription =
            healthCheckTopic.addMessageListener(message -> {
                log.debug("Received health check message from cluster");
                if (clusterManager.self().primary()) {
                    log.debug("Processing health check message");
                    nodeMonitoringService
                        .createOrUpdate(convert(message.content()))
                        .ignoreElement()
                        .onErrorResumeNext(throwable -> {
                            log.error("Unable to process health check message", throwable);
                            return Completable.complete();
                        })
                        .subscribe();
                }
            });
        monitorTopic = clusterManager.topic("node-monitor");
        monitorSubscriptionId =
            monitorTopic.addMessageListener(message -> {
                log.debug("Received monitor message from cluster");
                if (clusterManager.self().primary()) {
                    log.debug("Processing monitor message");
                    nodeMonitoringService
                        .createOrUpdate(convert(message.content()))
                        .ignoreElement()
                        .onErrorResumeNext(throwable -> {
                            log.error("Unable to process monitor message", throwable);
                            return Completable.complete();
                        })
                        .subscribe();
                }
            });
    }

    private void registerInternalListener() {
        nodeInfosMessageConsumer =
            vertx
                .eventBus()
                .localConsumer(
                    NodeInfosService.GIO_NODE_INFOS_BUS,
                    event -> {
                        log.debug("Received node infos message from internal bus");
                        nodeInfosTopic.publish(event.body());
                    }
                );
        healthCheckMessageConsumer =
            vertx
                .eventBus()
                .localConsumer(
                    NodeHealthCheckService.GIO_NODE_HEALTHCHECK_BUS,
                    event -> {
                        log.debug("Received health check message from internal bus");
                        healthCheckTopic.publish(event.body());
                    }
                );
        monitorMessageConsumer =
            vertx
                .eventBus()
                .localConsumer(
                    NodeMonitorService.GIO_NODE_MONITOR_BUS,
                    event -> {
                        log.debug("Received monitor message from internal bus");
                        monitorTopic.publish(event.body());
                    }
                );
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (nodeInfosMessageConsumer != null) {
            nodeInfosMessageConsumer.unregister();
        }
        if (healthCheckMessageConsumer != null) {
            healthCheckMessageConsumer.unregister();
        }
        if (monitorMessageConsumer != null) {
            monitorMessageConsumer.unregister();
        }

        if (clusterManager != null) {
            monitorTopic.removeMessageListener(nodeInfoSubscription);
            monitorTopic.removeMessageListener(healthCheckSubscription);
            monitorTopic.removeMessageListener(monitorSubscriptionId);
        }
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
            if (payload != null) {
                monitoring.setPayload(objectMapper.writeValueAsString(payload));
            }
        } catch (JsonProcessingException e) {
            log.error("An error occurred when trying to serialize monitoring payload to json");
        }

        return monitoring;
    }
}
