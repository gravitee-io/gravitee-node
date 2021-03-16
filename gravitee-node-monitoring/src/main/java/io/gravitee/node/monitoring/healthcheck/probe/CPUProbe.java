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
package io.gravitee.node.monitoring.healthcheck.probe;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.monitor.probe.ProcessProbe;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CPUProbe implements Probe {

    @Value("${services.health.threshold.cpu:80}")
    private int threshold;

    @Override
    public String id() {
        return "cpu";
    }

    @Override
    public boolean isVisibleByDefault() {
        return false;
    }

    @Override
    public CompletableFuture<Result> check() {
        try {
            return CompletableFuture.supplyAsync(() ->
                    ProcessProbe.getInstance().getProcessCpuPercent() < threshold
                    ? Result.healthy()
                    : Result.unhealthy(String.format("CPU percent is over the threshold of %d %%", threshold)));
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(Result.unhealthy(ex));
        }
    }
}
