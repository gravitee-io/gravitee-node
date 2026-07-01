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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.monitor.GpuInfo;
import io.gravitee.node.monitoring.monitor.gpu.GpuMetricsProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GpuProbeTest {

    @Test
    void should_return_empty_devices_when_no_provider_is_available() {
        GpuProbe probe = new GpuProbe(List.of(unavailableProvider()));

        GpuInfo info = probe.gpuInfo();

        assertThat(info.timestamp()).isPositive();
        assertThat(info.devices()).isEmpty();
    }

    @Test
    void should_use_first_available_provider() {
        GpuInfo.Device device = new GpuInfo.Device(0, "gpu-0", new GpuInfo.Mem(-1, -1));
        GpuProbe probe = new GpuProbe(List.of(unavailableProvider(), availableProvider(List.of(device))));

        GpuInfo info = probe.gpuInfo();

        assertThat(info.devices()).hasSize(1);
        assertThat(info.devices().get(0).index()).isEqualTo(0);
    }

    @Test
    void should_not_throw_when_provider_fails() {
        GpuProbe probe = new GpuProbe(
            List.of(
                new GpuMetricsProvider() {
                    @Override
                    public String name() {
                        return "boom";
                    }

                    @Override
                    public boolean isAvailable() {
                        throw new RuntimeException("boom");
                    }

                    @Override
                    public List<GpuInfo.Device> readDevices() {
                        return List.of();
                    }
                }
            )
        );

        assertThat(probe.gpuInfo().devices()).isEmpty();
    }

    private static GpuMetricsProvider unavailableProvider() {
        return new GpuMetricsProvider() {
            @Override
            public String name() {
                return "unavailable";
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public List<GpuInfo.Device> readDevices() {
                return List.of();
            }
        };
    }

    private static GpuMetricsProvider availableProvider(List<GpuInfo.Device> devices) {
        return new GpuMetricsProvider() {
            @Override
            public String name() {
                return "available";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<GpuInfo.Device> readDevices() {
                return devices;
            }
        };
    }
}
