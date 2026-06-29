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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.monitor.GpuInfo;
import io.gravitee.node.monitoring.eventbus.GpuInfoCodec;
import io.gravitee.node.monitoring.monitor.NodeGpuMonitorService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
class GpuMonitorEventHandlerTest {

    private Vertx vertx;
    private GpuSnapshotRegistry registry;
    private GpuMonitorEventHandler cut;

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        registry = new GpuSnapshotRegistry();
        cut = new GpuMonitorEventHandler(vertx, registry);
        cut.doStart();
    }

    @AfterEach
    void tearDown() throws Exception {
        cut.doStop();
        vertx.close();
    }

    @Test
    void should_update_registry_when_a_gpu_snapshot_is_published() {
        GpuInfo.Device device = new GpuInfo.Device(0, "NVIDIA A100", new GpuInfo.Mem(-1, -1));
        GpuInfo published = new GpuInfo(123L, List.of(device));

        vertx
            .eventBus()
            .registerCodec(new GpuInfoCodec())
            .send(NodeGpuMonitorService.GIO_NODE_GPU_BUS, published, new DeliveryOptions().setCodecName(GpuInfoCodec.CODEC_NAME));

        awaitDevices(1);

        assertThat(registry.current().devices()).hasSize(1);
        assertThat(registry.current().devices().get(0).name()).isEqualTo("NVIDIA A100");
    }

    private void awaitDevices(int expectedSize) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (registry.current().devices().size() != expectedSize && System.nanoTime() < deadline) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Test
    void should_default_to_empty_devices_before_any_message() {
        assertThat(registry.current()).isNotNull();
        assertThat(registry.current().devices()).isEmpty();
    }
}
