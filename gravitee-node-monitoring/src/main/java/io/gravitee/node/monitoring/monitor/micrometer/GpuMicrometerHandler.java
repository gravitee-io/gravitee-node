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
package io.gravitee.node.monitoring.monitor.micrometer;

import io.gravitee.node.api.monitor.GpuInfo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Binds GPU metrics to a Micrometer registry, the same way
 * {@code NodeHealthCheckMicrometerHandler} binds health probes. Gauges are
 * registered once per device discovered at bind time and read their value from
 * the latest snapshot supplied by {@link #snapshotSupplier}, which is refreshed
 * periodically by {@code NodeGpuMetricsService}.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class GpuMicrometerHandler implements MeterBinder {

    private final Supplier<GpuInfo> snapshotSupplier;

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        GpuInfo snapshot = snapshotSupplier.get();
        if (snapshot == null || snapshot.devices() == null) {
            return;
        }
        for (GpuInfo.Device device : snapshot.devices()) {
            Tags tags = Tags.of("gpu", String.valueOf(device.index()));
            if (device.name() != null) {
                tags = tags.and("name", device.name());
            }
            if (device.uuid() != null) {
                tags = tags.and("uuid", device.uuid());
            }

            register(registry, "gpu.utilization", "percent", tags, device.index(), GpuInfo.Device::utilizationPercent);
            register(registry, "gpu.memory.utilization", "percent", tags, device.index(), GpuInfo.Device::memoryUtilizationPercent);
            register(registry, "gpu.memory.used", "bytes", tags, device.index(), d -> d.mem().getUsed());
            register(registry, "gpu.memory.total", "bytes", tags, device.index(), d -> d.mem().total());
            register(registry, "gpu.temperature", "celsius", tags, device.index(), GpuInfo.Device::temperature);
            register(registry, "gpu.power", "watts", tags, device.index(), GpuInfo.Device::powerWatts);
        }
    }

    /**
     * Registers a gauge for the given metric, but only when the device currently exposes it
     * ({@code value >= 0}). This avoids publishing constant {@code -1} series for metrics a
     * provider cannot collect (e.g. a provider that only reports device identity and VRAM).
     */
    private void register(
        MeterRegistry registry,
        String name,
        String unit,
        Tags tags,
        int index,
        ToDoubleFunction<GpuInfo.Device> accessor
    ) {
        if (deviceValue(index, accessor) < 0) {
            return;
        }
        Gauge
            .builder(name, snapshotSupplier, supplier -> deviceValue(index, accessor))
            .tags(tags)
            .baseUnit(unit)
            .description("GPU metrics of the node")
            .register(registry);
    }

    private double deviceValue(int index, ToDoubleFunction<GpuInfo.Device> accessor) {
        GpuInfo snapshot = snapshotSupplier.get();
        if (snapshot == null || snapshot.devices() == null) {
            return Double.NaN;
        }
        return snapshot.devices().stream().filter(d -> d.index() == index).findFirst().map(accessor::applyAsDouble).orElse(Double.NaN);
    }
}
