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

import static io.gravitee.node.monitoring.MonitoringConstants.*;

import io.gravitee.alert.api.event.DefaultEvent;
import io.gravitee.alert.api.event.Event;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.DefaultProbeEvaluator;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.eventbus.MessageProducer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class NodeHealthCheckThread implements Runnable {

    public static final int TIMEOUT_MS = 30000;
    private final DefaultProbeEvaluator probeRegistry;
    private final AlertEventProducer alertEventProducer;
    private final MessageProducer<HealthCheck> producer;
    private final Node node;
    private final NodeHealthCheckService nodeHealthCheckService;

    private long timestamp;

    @Override
    public void run() {
        try {
            this.timestamp = System.currentTimeMillis();
            final Map<Probe, Result> results = probeRegistry.evaluate().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // We want to propagate health-check with visible probes only.
            final HealthCheck healthCheck = getHealthCheck(results);

            // Check memory pressure
            checkGcPressure(results);

            producer.write(healthCheck);
            sendAlertEngineEvent(healthCheck);
        } catch (Exception e) {
            log.error("An error occurred when trying to evaluate health check probes.", e);
            Thread.currentThread().interrupt();
        }
    }

    private void checkGcPressure(Map<Probe, Result> results) {
        boolean noPressure = results
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().id().equals("gc-pressure"))
            .allMatch(probeResultEntry -> probeResultEntry.getValue().isHealthy());
        nodeHealthCheckService.setGcPressureTooHigh(!noPressure);
    }

    private void sendAlertEngineEvent(HealthCheck healthCheck) {
        DefaultEvent.Builder builder = Event.now().type(NODE_HEALTHCHECK);

        builder.property(PROPERTY_NODE_ID, node.id());
        builder.property(PROPERTY_NODE_HOSTNAME, node.hostname());
        builder.property(PROPERTY_NODE_APPLICATION, node.application());
        builder.property(PROPERTY_NODE_HEALTHY, String.valueOf(healthCheck.isHealthy()));
        builder.organizations((Set<String>) node.metadata().get(Node.META_ORGANIZATIONS));
        builder.environments((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS));

        healthCheck
            .getResults()
            .forEach((probeId, result) -> {
                builder.property(PROPERTY_PROBE_SUFFIX + probeId, result.isHealthy());
                if (!result.isHealthy()) {
                    builder.property(PROPERTY_PROBE_SUFFIX + probeId + ".message", result.getMessage());
                }
            });

        alertEventProducer.send(builder.build());
    }

    /**
     * Returns the health-check with only probes that are visible by default.
     *
     * @return health-check with all probes which are visible by default.
     */
    private HealthCheck getHealthCheck(Map<Probe, Result> results) {
        Map<String, Result> visibleResults = results
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().isVisibleByDefault())
            .collect(Collectors.toMap(probeResultEntry -> probeResultEntry.getKey().id(), Map.Entry::getValue));

        return new HealthCheck(timestamp, visibleResults);
    }
}
