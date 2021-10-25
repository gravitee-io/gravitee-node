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
package io.gravitee.node.monitoring.healthcheck;

import io.gravitee.alert.api.event.DefaultEvent;
import io.gravitee.alert.api.event.Event;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageProducer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthCheckThread implements Handler<Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeHealthCheckThread.class);

    private static final String NODE_HEALTHCHECK = "NODE_HEALTHCHECK";

    private static final String PROPERTY_NODE_HOSTNAME = "node.hostname";
    private static final String PROPERTY_NODE_APPLICATION = "node.application";
    private static final String PROPERTY_NODE_ID = "node.id";
    private static final String PROPERTY_NODE_HEALTHY = "node.healthy";
    private static final String PROPERTY_PROBE_SUFFIX = "node.probe.";

    @Autowired
    private AlertEventProducer eventProducer;

    @Autowired
    private Node node;

    private long timestamp;

    private final Map<Probe, Result> results;

    private final MessageProducer<HealthCheck> producer;

    public NodeHealthCheckThread(List<Probe> probes, MessageProducer<HealthCheck> producer) {
        this.results = probes.stream().collect(Collectors.toMap(probe -> probe, probe -> Result.notReady()));
        this.producer = producer;
    }

    @Override
    public void handle(Long tick) {
        this.timestamp = System.currentTimeMillis();

        for (Map.Entry<Probe, Result> probe : results.entrySet()) {
            try {
                probe.getKey().check().thenAccept(probe::setValue);
            } catch (Exception ex) {
                LOGGER.error(
                    "An error occurred when trying to evaluate health check probe {}. Switching probe to unhealthy.",
                    probe.getKey(),
                    ex
                );
                probe.setValue(Result.unhealthy(ex));
            }
        }

        // We want to propagate health-check with visible probes only.
        final HealthCheck healthCheck = getHealthCheck(true);
        producer.write(healthCheck);
        sendAlertEngineEvent(healthCheck);
    }

    private void sendAlertEngineEvent(HealthCheck healthCheck) {
        DefaultEvent.Builder builder = Event.now().type(NODE_HEALTHCHECK);

        builder.property(PROPERTY_NODE_ID, node.id());
        builder.property(PROPERTY_NODE_HOSTNAME, node.hostname());
        builder.property(PROPERTY_NODE_APPLICATION, node.application());
        builder.property(PROPERTY_NODE_HEALTHY, String.valueOf(healthCheck.isHealthy()));

        healthCheck
            .getResults()
            .forEach(
                (probeId, result) -> {
                    builder.property(PROPERTY_PROBE_SUFFIX + probeId, result.isHealthy());
                    if (!result.isHealthy()) {
                        builder.property(PROPERTY_PROBE_SUFFIX + probeId + ".message", result.getMessage());
                    }
                }
            );

        eventProducer.send(builder.build());
    }

    /**
     * Returns the health-check with all probes which are visible by default.
     *
     * @return health-check with all probes which are visible by default.
     */
    public HealthCheck getHealthCheck() {
        return getHealthCheck(false);
    }

    /**
     * Returns the health-check with either all probes either only visible by default ones.
     *
     * @param filterVisibleByDefault flag indicating if probes must be filtered to visible by default or not.
     * @return health-check with all probes which are visible by default.
     */
    public HealthCheck getHealthCheck(boolean filterVisibleByDefault) {
        Map<String, Result> results =
            this.results.entrySet()
                .stream()
                .filter(entry -> !filterVisibleByDefault || entry.getKey().isVisibleByDefault())
                .collect(Collectors.toMap(probeResultEntry -> probeResultEntry.getKey().id(), Map.Entry::getValue));

        return new HealthCheck(timestamp, results);
    }

    public Map<Probe, Result> getResults() {
        return results;
    }
}
