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
package io.gravitee.node.healthcheck.micrometer;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.healthcheck.ProbeStatusRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.vertx.core.Handler;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthcheckMetrics implements MeterBinder, Handler<Long> {

    private final ProbeStatusRegistry statusRegistry;

    public NodeHealthcheckMetrics(ProbeStatusRegistry statusRegistry) {
        this.statusRegistry = statusRegistry;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Map.Entry<Probe, Result> status: statusRegistry.getResults().entrySet()) {
            Gauge
                    .builder("node", status.getKey(), probe -> status.getValue().isHealthy() ? 1 : 0)
                    .tag("probe", status.getKey().id())
                    .description("The health-check of the " + status.getKey().id() + " probe")
                    .baseUnit("health")
                    .register(registry);
        }
    }

    @Override
    public void handle(Long tick) {
        for (Map.Entry<Probe, Result> probe : statusRegistry.getResults().entrySet()) {
            try {
                probe.getKey().check().thenAccept(probe::setValue);
            } catch (Exception ex) {
                ex.printStackTrace();
                probe.setValue(Result.unhealthy(ex));
            }
        }
    }
}
