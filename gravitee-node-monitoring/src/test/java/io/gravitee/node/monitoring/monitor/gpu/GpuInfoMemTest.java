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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GpuInfoMemTest {

    @Test
    void should_compute_used_and_percentages_when_available() {
        GpuInfo.Mem mem = new GpuInfo.Mem(1000, 400);

        assertThat(mem.getUsed()).isEqualTo(600);
        assertThat(mem.getUsedPercent()).isEqualTo((short) 60);
        assertThat(mem.getFreePercent()).isEqualTo((short) 40);
    }

    @Test
    void should_return_minus_one_percentages_when_values_are_unavailable() {
        // free unavailable -> used unknown -> used percent unavailable
        assertThat(new GpuInfo.Mem(1000, -1).getUsedPercent()).isEqualTo((short) -1);
        assertThat(new GpuInfo.Mem(1000, -1).getFreePercent()).isEqualTo((short) -1);
        // total unavailable -> both unavailable
        assertThat(new GpuInfo.Mem(-1, -1).getUsedPercent()).isEqualTo((short) -1);
        assertThat(new GpuInfo.Mem(-1, -1).getFreePercent()).isEqualTo((short) -1);
    }
}
