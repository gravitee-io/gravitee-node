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
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.monitoring.eventbus.HealthCheckCodec;
import io.gravitee.node.monitoring.healthcheck.micrometer.NodeHealthCheckMicrometerHandler;
import io.gravitee.node.monitoring.healthcheck.probe.CPUProbe;
import io.gravitee.node.monitoring.healthcheck.probe.MemoryProbe;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.micrometer.backends.BackendRegistries;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthCheckService extends AbstractService {

    public static final String GIO_NODE_HEALTHCHECK_BUS = "gio:node:healthcheck";

    @Autowired
    private ManagementEndpointManager managementEndpointManager;

    @Autowired
    private ProbeManager probeManager;

    @Autowired
    private NodeHealthCheckManagementEndpoint healthCheckEndpoint;

    @Autowired
    private Vertx vertx;

    private long metricsPollerId = -1;

    private static final long NODE_CHECKER_DELAY = 5000;

    private MessageProducer<HealthCheck> producer;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        producer = vertx.eventBus()
                .registerCodec(new HealthCheckCodec())
                .sender(GIO_NODE_HEALTHCHECK_BUS, new DeliveryOptions()
                        .setTracingPolicy(TracingPolicy.IGNORE)
                        .setCodecName(HealthCheckCodec.CODEC_NAME));

        // Poll data
        NodeHealthCheckThread statusRegistry = new NodeHealthCheckThread(probeManager.getProbes(), producer);

        applicationContext.getAutowireCapableBeanFactory().autowireBean(statusRegistry);

        metricsPollerId = vertx.setPeriodic(NODE_CHECKER_DELAY, statusRegistry);

        healthCheckEndpoint.setRegistry(statusRegistry);
        managementEndpointManager.register(healthCheckEndpoint);

        MeterRegistry registry = BackendRegistries.getDefaultNow();

        if (registry instanceof PrometheusMeterRegistry) {
            new NodeHealthCheckMicrometerHandler(statusRegistry).bindTo(registry);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (metricsPollerId > 0) {
            vertx.cancelTimer(metricsPollerId);
        }

        producer.close();
    }

    @Override
    protected String name() {
        return "Node Health-check service";
    }
}
