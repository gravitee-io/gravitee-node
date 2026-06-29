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
package io.gravitee.node.api.monitor;

import java.io.Serializable;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public record GpuInfo(long timestamp, List<Device> devices) implements Serializable {
    public record Device(
        int index,
        String name,
        String uuid,
        String driverVersion,
        short utilizationPercent,
        short memoryUtilizationPercent,
        Mem mem,
        short temperature,
        double powerWatts
    )
        implements Serializable {
        /** Convenience constructor for providers that only report device identity. */
        public Device(int index, String name, Mem mem) {
            this(index, name, null, null, (short) -1, (short) -1, mem, (short) -1, -1d);
        }
    }

    public record Mem(long total, long free) implements Serializable {
        public long getUsed() {
            return total < 0 || free < 0 ? -1 : total - free;
        }

        public short getUsedPercent() {
            return calculatePercentage(getUsed(), total);
        }

        public short getFreePercent() {
            return calculatePercentage(free, total);
        }

        private static short calculatePercentage(long used, long max) {
            return max <= 0 ? 0 : (short) (Math.round((100d * used) / max));
        }
    }
}
