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
package io.gravitee.node.monitoring.monitor.gpu;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.monitor.GpuInfo;
import io.gravitee.node.monitoring.monitor.NodeGpuMonitorService;
import io.gravitee.node.monitoring.monitor.micrometer.GpuMicrometerHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Consumer of the GPU producer/consumer pipeline. It listens to the GPU snapshots published by
 * {@link NodeGpuMonitorService} on the event bus, updates the shared {@link GpuSnapshotRegistry}
 * (read by the node monitor, the {@code /monitor} endpoint and the Micrometer gauges), and binds the
 * {@link GpuMicrometerHandler} to the registry once devices are discovered.
 *
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class GpuMonitorEventHandler extends AbstractService<GpuMonitorEventHandler> {

    private final Vertx vertx;
    private final GpuSnapshotRegistry registry;

    private MessageConsumer<GpuInfo> consumer;
    private boolean micrometerBound;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        consumer =
            vertx
                .eventBus()
                .localConsumer(
                    NodeGpuMonitorService.GIO_NODE_GPU_BUS,
                    event -> {
                        GpuInfo info = event.body();
                        registry.update(info);
                        bindMicrometer(info);
                    }
                );
    }

    private void bindMicrometer(GpuInfo info) {
        if (micrometerBound || info == null || info.devices() == null || info.devices().isEmpty()) {
            return;
        }
        MeterRegistry meterRegistry = BackendRegistries.getDefaultNow();
        if (meterRegistry == null) {
            log.warn("Metrics registry is not available, GPU metrics will not be exposed (services.metrics.enabled=true)");
            return;
        }
        // Gauges read the live snapshot from the registry; bind once, devices are stable.
        new GpuMicrometerHandler(registry::current).bindTo(meterRegistry);
        micrometerBound = true;
        log.info("GPU metrics bound to Micrometer for {} device(s)", info.devices().size());
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (consumer != null) {
            consumer.unregister();
        }
    }

    @Override
    protected String name() {
        return "Node GPU Monitor Event Handler";
    }
}
