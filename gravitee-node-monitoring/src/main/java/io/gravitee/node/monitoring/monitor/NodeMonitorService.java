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
package io.gravitee.node.monitoring.monitor;

import static io.gravitee.node.monitoring.MonitoringConstants.*;

import io.gravitee.alert.api.event.Event;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.monitoring.eventbus.MonitorCodec;
import io.gravitee.node.monitoring.spring.MonitoringConfiguration;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.tracing.TracingPolicy;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class NodeMonitorService extends AbstractService<NodeMonitorService> {

    public static final String GIO_NODE_MONITOR_BUS = "gio:node:monitor";

    private final NodeMonitorManagementEndpoint nodeMonitorManagementEndpoint;
    private final ManagementEndpointManager managementEndpointManager;
    private final AlertEventProducer alertEventProducer;
    private final Node node;
    private final Vertx vertx;
    private final MonitoringConfiguration monitoringConfiguration;

    private MessageProducer<Monitor> producer;
    private ExecutorService executorService;

    @Override
    protected void doStart() throws Exception {
        if (monitoringConfiguration.enabled()) {
            super.doStart();

            executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "node-monitor"));

            producer =
                vertx
                    .eventBus()
                    .registerCodec(new MonitorCodec())
                    .sender(
                        GIO_NODE_MONITOR_BUS,
                        new DeliveryOptions().setTracingPolicy(TracingPolicy.IGNORE).setCodecName(MonitorCodec.CODEC_NAME)
                    );

            NodeMonitorThread monitorThread = new NodeMonitorThread(producer, node, alertEventProducer);

            // Send an event to notify about the node status
            alertEventProducer.send(
                Event
                    .now()
                    .type(NODE_LIFECYCLE)
                    .property(PROPERTY_NODE_EVENT, NODE_EVENT_START)
                    .property(PROPERTY_NODE_ID, node.id())
                    .property(PROPERTY_NODE_HOSTNAME, node.hostname())
                    .property(PROPERTY_NODE_APPLICATION, node.application())
                    .organizations((Set<String>) node.metadata().get(Node.META_ORGANIZATIONS))
                    .environments((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS))
                    .build()
            );

            log.info(
                "Node monitoring scheduled with fixed delay {} {} ",
                monitoringConfiguration.delay(),
                monitoringConfiguration.unit().name()
            );

            ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                    monitorThread,
                    0,
                    monitoringConfiguration.delay(),
                    monitoringConfiguration.unit()
                );

            managementEndpointManager.register(nodeMonitorManagementEndpoint);
        }
    }

    @Override
    public NodeMonitorService preStop() throws Exception {
        if (monitoringConfiguration.enabled()) {
            // Send an event to notify about the node status
            alertEventProducer.send(
                Event
                    .now()
                    .type(NODE_LIFECYCLE)
                    .property(PROPERTY_NODE_EVENT, NODE_EVENT_STOP)
                    .property(PROPERTY_NODE_ID, node.id())
                    .property(PROPERTY_NODE_HOSTNAME, node.hostname())
                    .property(PROPERTY_NODE_APPLICATION, node.application())
                    .organizations((Set<String>) node.metadata().get(Node.META_ORGANIZATIONS))
                    .environments((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS))
                    .build()
            );
        }

        return this;
    }

    @Override
    protected void doStop() throws Exception {
        if (monitoringConfiguration.enabled()) {
            if (executorService != null && !executorService.isShutdown()) {
                log.info("Stop node monitor");
                executorService.shutdownNow();
            } else {
                log.info("Node monitor already shutdown");
            }

            super.doStop();

            log.info("Stop node monitor : DONE");

            if (producer != null) {
                producer.close();
            }
        }
    }

    @Override
    protected String name() {
        return "Node Monitor Service";
    }
}
