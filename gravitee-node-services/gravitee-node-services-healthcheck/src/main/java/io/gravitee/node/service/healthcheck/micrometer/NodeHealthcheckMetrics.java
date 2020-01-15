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
package io.gravitee.node.service.healthcheck.micrometer;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.vertx.core.Handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthcheckMetrics implements MeterBinder, Handler<Long> {

    private final Map<Probe, Result> probes;

    public NodeHealthcheckMetrics(List<Probe> probes) {
        this.probes = probes.stream().collect(Collectors.toMap(probe -> probe, probe -> Result.healthy()));
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Probe probe: probes.keySet()) {
            Gauge.builder("node", probe, probe1 -> {
                Result check = probes.get(probe1);
                return check.isHealthy() ? 1 : 0;
            }).tag("probe", probe.id()).description("The health-check of the " + probe.id() + " probe").baseUnit("health").register(registry);
        }
    }

    @Override
    public void handle(Long tick) {
        for (Map.Entry<Probe, Result> probe : probes.entrySet()) {
            try {
                probe.getKey().check().thenAccept(probe::setValue);
            } catch (Exception ex) {
                ex.printStackTrace();
                probe.setValue(Result.unhealthy(ex));
            }
        }
    }
}
