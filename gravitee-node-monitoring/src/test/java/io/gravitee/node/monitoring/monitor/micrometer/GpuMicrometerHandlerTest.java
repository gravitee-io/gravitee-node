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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.monitor.GpuInfo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
class GpuMicrometerHandlerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private static GpuInfo.Device nvidiaDevice() {
        GpuInfo.Mem mem = new GpuInfo.Mem(40960L * 1024 * 1024, 30960L * 1024 * 1024);
        return new GpuInfo.Device(0, "NVIDIA A100", "GPU-abc", "535.104.05", (short) 42, (short) 17, mem, (short) 55, 250.43);
    }

    private static GpuInfo.Device identityOnlyDevice() {
        // a provider that reports only identity, everything else N/A (-1)
        return new GpuInfo.Device(0, "Some GPU", new GpuInfo.Mem(-1, -1));
    }

    @Test
    void should_register_all_gauges_for_a_full_device() {
        AtomicReference<GpuInfo> snapshot = new AtomicReference<>(new GpuInfo(1L, List.of(nvidiaDevice())));

        new GpuMicrometerHandler(snapshot::get).bindTo(registry);

        assertThat(registry.find("gpu.utilization").gauge().value()).isEqualTo(42d);
        assertThat(registry.find("gpu.memory.utilization").gauge().value()).isEqualTo(17d);
        assertThat(registry.find("gpu.memory.used").gauge().value()).isEqualTo((double) (40960L - 30960L) * 1024 * 1024);
        assertThat(registry.find("gpu.memory.total").gauge().value()).isEqualTo((double) 40960L * 1024 * 1024);
        assertThat(registry.find("gpu.temperature").gauge().value()).isEqualTo(55d);
        assertThat(registry.find("gpu.power").gauge().value()).isEqualTo(250.43);
    }

    @Test
    void should_tag_gauges_with_gpu_index_name_and_uuid() {
        AtomicReference<GpuInfo> snapshot = new AtomicReference<>(new GpuInfo(1L, List.of(nvidiaDevice())));

        new GpuMicrometerHandler(snapshot::get).bindTo(registry);

        Gauge gauge = registry.find("gpu.utilization").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.getId().getTag("gpu")).isEqualTo("0");
        assertThat(gauge.getId().getTag("name")).isEqualTo("NVIDIA A100");
        assertThat(gauge.getId().getTag("uuid")).isEqualTo("GPU-abc");
        assertThat(gauge.getId().getBaseUnit()).isEqualTo("percent");
    }

    @Test
    void should_reflect_latest_snapshot_on_scrape() {
        AtomicReference<GpuInfo> snapshot = new AtomicReference<>(new GpuInfo(1L, List.of(nvidiaDevice())));
        new GpuMicrometerHandler(snapshot::get).bindTo(registry);

        // simulate the scheduled refresh updating utilization to 88%
        GpuInfo.Mem mem = new GpuInfo.Mem(40960L * 1024 * 1024, 30960L * 1024 * 1024);
        GpuInfo.Device updated = new GpuInfo.Device(
            0,
            "NVIDIA A100",
            "GPU-abc",
            "535.104.05",
            (short) 88,
            (short) 17,
            mem,
            (short) 60,
            300d
        );
        snapshot.set(new GpuInfo(2L, List.of(updated)));

        assertThat(registry.find("gpu.utilization").gauge().value()).isEqualTo(88d);
        assertThat(registry.find("gpu.temperature").gauge().value()).isEqualTo(60d);
    }

    @Test
    void should_skip_unavailable_metrics_for_identity_only_device() {
        AtomicReference<GpuInfo> snapshot = new AtomicReference<>(new GpuInfo(1L, List.of(identityOnlyDevice())));

        new GpuMicrometerHandler(snapshot::get).bindTo(registry);

        // identity-only device exposes nothing (all values are -1)
        assertThat(registry.find("gpu.utilization").gauge()).isNull();
        assertThat(registry.find("gpu.memory.total").gauge()).isNull();
        assertThat(registry.find("gpu.temperature").gauge()).isNull();
        assertThat(registry.find("gpu.power").gauge()).isNull();
    }

    @Test
    void should_register_one_set_of_gauges_per_device() {
        GpuInfo.Mem mem = new GpuInfo.Mem(40960L * 1024 * 1024, 30960L * 1024 * 1024);
        GpuInfo.Device second = new GpuInfo.Device(1, "NVIDIA A100", "GPU-def", "535.104.05", (short) 10, (short) 5, mem, (short) 40, 100d);
        AtomicReference<GpuInfo> snapshot = new AtomicReference<>(new GpuInfo(1L, List.of(nvidiaDevice(), second)));

        new GpuMicrometerHandler(snapshot::get).bindTo(registry);

        assertThat(registry.find("gpu.utilization").gauges()).hasSize(2);
        assertThat(registry.find("gpu.utilization").tag("gpu", "1").gauge().value()).isEqualTo(10d);
    }

    @Test
    void should_not_fail_on_empty_or_null_snapshot() {
        new GpuMicrometerHandler(() -> new GpuInfo(1L, List.of())).bindTo(registry);
        new GpuMicrometerHandler(() -> null).bindTo(registry);

        assertThat(registry.getMeters()).isEmpty();
    }
}
