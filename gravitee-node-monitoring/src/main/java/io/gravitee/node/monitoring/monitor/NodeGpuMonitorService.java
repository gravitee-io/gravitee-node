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

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.monitor.GpuInfo;
import io.gravitee.node.monitoring.eventbus.GpuInfoCodec;
import io.gravitee.node.monitoring.monitor.probe.GpuProbe;
import io.gravitee.node.monitoring.spring.GpuConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.tracing.TracingPolicy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Producer of the GPU producer/consumer pipeline: a scheduled thread collects the GPU snapshot
 * (via {@link GpuProbe}) once per monitoring interval and publishes it on the event bus. Consumers
 * ({@code GpuMonitorEventHandler}, ultimately the Micrometer gauges and the node monitor) read it
 * from there, so the GPU is probed a single time per interval. This mirrors {@code NodeMonitorService}
 * producing {@code Monitor} objects on {@code gio:node:monitor}.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class NodeGpuMonitorService extends AbstractService<NodeGpuMonitorService> {

    public static final String GIO_NODE_GPU_BUS = "gio:node:gpu";

    private final Vertx vertx;
    private final GpuConfiguration gpuConfiguration;

    private MessageProducer<GpuInfo> producer;
    private ScheduledExecutorService executorService;

    @Override
    protected void doStart() throws Exception {
        if (!gpuConfiguration.enabled()) {
            return;
        }
        super.doStart();

        producer =
            vertx
                .eventBus()
                .registerCodec(new GpuInfoCodec())
                .sender(
                    GIO_NODE_GPU_BUS,
                    new DeliveryOptions().setTracingPolicy(TracingPolicy.IGNORE).setCodecName(GpuInfoCodec.CODEC_NAME)
                );

        executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "node-gpu-monitor"));
        executorService.scheduleWithFixedDelay(this::collectAndPublish, 0, gpuConfiguration.delay(), gpuConfiguration.unit());

        log.info("Node GPU monitoring scheduled with fixed delay {} {} ", gpuConfiguration.delay(), gpuConfiguration.unit().name());
    }

    private void collectAndPublish() {
        try {
            producer.write(GpuProbe.getInstance().gpuInfo());
        } catch (Exception e) {
            log.warn("Unable to collect and publish GPU metrics", e);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (executorService != null && !executorService.isShutdown()) {
            log.info("Stop node GPU monitor");
            executorService.shutdownNow();
        }
        super.doStop();

        if (producer != null) {
            producer.close();
        }
    }

    @Override
    protected String name() {
        return "Node GPU Monitor Service";
    }
}
