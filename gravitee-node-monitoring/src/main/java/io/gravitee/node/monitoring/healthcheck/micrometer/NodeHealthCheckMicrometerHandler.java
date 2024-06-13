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
package io.gravitee.node.monitoring.healthcheck.micrometer;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.DefaultProbeEvaluator;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class NodeHealthCheckMicrometerHandler implements MeterBinder {

    private final DefaultProbeEvaluator probeRegistry;

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        try {
            for (Map.Entry<Probe, Result> entry : probeRegistry.evaluate().get().entrySet()) {
                Gauge
                    .builder("node", entry, e -> e.getValue().isHealthy() ? 1d : 0d)
                    .tag("probe", entry.getKey().id())
                    .description("The health-check probes of the node")
                    .baseUnit("health")
                    .register(registry);
            }
        } catch (Exception e) {
            log.error("An error occurred while bind the health probes to micrometer");
        }
    }
}
