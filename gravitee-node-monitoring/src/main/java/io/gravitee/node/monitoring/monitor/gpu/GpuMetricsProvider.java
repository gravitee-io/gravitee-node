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

import io.gravitee.node.api.monitor.GpuInfo;
import java.util.List;

/**
 * SPI for collecting GPU metrics from a given vendor/back-end (e.g. NVIDIA via
 * {@code nvidia-smi}, ...). Implementations must be cheap to probe
 * for availability and must never throw from {@link #readDevices()}.
 *
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GpuMetricsProvider {
    /**
     * @return a short name identifying the provider (e.g. {@code "nvidia-smi"}).
     */
    String name();

    /**
     * @return {@code true} if this provider can collect metrics on the current host.
     */
    boolean isAvailable();

    /**
     * Collects one {@link GpuInfo.Device} per detected device.
     *
     * @return the list of devices, never {@code null}; an empty list when nothing could be read.
     */
    List<GpuInfo.Device> readDevices();
}
