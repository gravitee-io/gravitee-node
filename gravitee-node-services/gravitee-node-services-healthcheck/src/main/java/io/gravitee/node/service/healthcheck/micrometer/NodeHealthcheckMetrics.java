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
import io.gravitee.node.service.healthcheck.vertx.VertxCompletableFuture;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToDoubleFunction;

public class NodeHealthcheckMetrics implements MeterBinder {

    private final List<Probe> probes;

    public NodeHealthcheckMetrics(List<Probe> probes) {
        this.probes = probes;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Probe probe: probes) {
            Gauge.builder("node", probe, new ToDoubleFunction<Probe>() {
                @Override
                public double applyAsDouble(Probe probe) {
                    try {
                        CompletableFuture<Result> probeFuture = probe.check();

                        if (VertxCompletableFuture.class.getSimpleName().equals(probeFuture.getClass().getSimpleName())) {
                            return 1;
                        }

                        Result result = probeFuture.get();
                        return result.isHealthy() ? 1 : 0;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    return 0;
                }
            }).tag("probe", probe.id()).description("The health-check of the " + probe.id() + " probe").baseUnit("health").register(registry);
        }
    }
}
