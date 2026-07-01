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
package io.gravitee.node.monitoring.monitor.probe;

import io.gravitee.node.api.monitor.GpuInfo;
import io.gravitee.node.monitoring.monitor.gpu.GpuMetricsProvider;
import io.gravitee.node.monitoring.monitor.gpu.NvidiaSmiGpuMetricsProvider;
import java.util.List;
import lombok.CustomLog;

/**
 * Aggregates GPU metrics across the available {@link GpuMetricsProvider}s. Follows
 * the holder-singleton pattern used by {@code OsProbe}, {@code JvmProbe} and
 * {@code ProcessProbe}. When no provider can collect metrics, {@link #gpuInfo()}
 * returns a {@link GpuInfo} with an empty device list (never throws).
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class GpuProbe {

    private final List<GpuMetricsProvider> providers;

    private GpuProbe() {
        this(List.of(new NvidiaSmiGpuMetricsProvider()));
    }

    GpuProbe(List<GpuMetricsProvider> providers) {
        this.providers = providers;
    }

    private static class GpuProbeHolder {

        private static final GpuProbe INSTANCE = new GpuProbe();
    }

    public static GpuProbe getInstance() {
        return GpuProbeHolder.INSTANCE;
    }

    public GpuInfo gpuInfo() {
        long timestamp = System.currentTimeMillis();
        List<GpuInfo.Device> devices = List.of();

        for (GpuMetricsProvider provider : providers) {
            try {
                if (provider.isAvailable()) {
                    devices = provider.readDevices();
                    // first available provider wins
                    break;
                }
            } catch (Exception ex) {
                log.debug("GPU provider {} failed", provider.name(), ex);
            }
        }

        return new GpuInfo(timestamp, devices);
    }
}
