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

import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckThread;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthCheckMicrometerHandler implements MeterBinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeHealthCheckMicrometerHandler.class);

    private final NodeHealthCheckThread statusRegistry;

    public NodeHealthCheckMicrometerHandler(NodeHealthCheckThread statusRegistry) {
        this.statusRegistry = statusRegistry;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        for (Map.Entry<String, Result> status : statusRegistry.getHealthCheck().getResults().entrySet()) {
            Gauge
                .builder("node", status.getKey(), probe -> status.getValue().isHealthy() ? 1 : 0)
                .tag("probe", status.getKey())
                .description("The health-check of the " + status.getKey() + " probe")
                .baseUnit("health")
                .register(registry);
        }
    }
}
