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

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.monitoring.DefaultProbeEvaluator;
import io.gravitee.node.monitoring.eventbus.HealthCheckCodec;
import io.gravitee.node.monitoring.healthcheck.micrometer.NodeHealthCheckMicrometerHandler;
import io.gravitee.node.monitoring.spring.HealthConfiguration;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class NodeHealthCheckService extends AbstractService {

    public static final String GIO_NODE_HEALTHCHECK_BUS = "gio:node:healthcheck";

    private final ManagementEndpointManager managementEndpointManager;
    private final DefaultProbeEvaluator probeRegistry;
    private final NodeHealthCheckManagementEndpoint healthCheckEndpoint;
    private final AlertEventProducer alertEventProducer;
    private final Node node;
    private final Vertx vertx;
    private final HealthConfiguration healthConfiguration;

    private MessageProducer<HealthCheck> producer;
    private ExecutorService executorService;

    @Override
    protected void doStart() throws Exception {
        if (healthConfiguration.enabled()) {
            super.doStart();

            executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "node-health-check"));

            producer =
                vertx
                    .eventBus()
                    .registerCodec(new HealthCheckCodec())
                    .sender(
                        GIO_NODE_HEALTHCHECK_BUS,
                        new DeliveryOptions().setTracingPolicy(TracingPolicy.IGNORE).setCodecName(HealthCheckCodec.CODEC_NAME)
                    );

            final NodeHealthCheckThread nodeHealthCheckThread = new NodeHealthCheckThread(
                probeRegistry,
                alertEventProducer,
                producer,
                node
            );

            managementEndpointManager.register(healthCheckEndpoint);

            MeterRegistry micrometerRegistry = BackendRegistries.getDefaultNow();

            if (micrometerRegistry instanceof PrometheusMeterRegistry) {
                new NodeHealthCheckMicrometerHandler(probeRegistry).bindTo(micrometerRegistry);
            }

            ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                    nodeHealthCheckThread,
                    0,
                    healthConfiguration.delay(),
                    healthConfiguration.unit()
                );

            log.info("Node health check scheduled with fixed delay {} {} ", healthConfiguration.delay(), healthConfiguration.unit().name());
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (executorService != null && !executorService.isShutdown()) {
            log.info("Stop node health check");
            executorService.shutdownNow();
        } else {
            log.info("Node health check already shutdown");
        }

        if (producer != null) {
            producer.close();
        }
    }

    @Override
    protected String name() {
        return "Node Health-check service";
    }
}
