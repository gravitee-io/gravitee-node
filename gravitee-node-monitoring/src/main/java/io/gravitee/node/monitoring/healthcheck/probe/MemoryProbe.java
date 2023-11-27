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

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.monitor.probe.JvmProbe;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MemoryProbe implements Probe {

    @Autowired
    private Configuration configuration;

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public boolean isVisibleByDefault() {
        return false;
    }

    @Override
    public CompletableFuture<Result> check() {
        try {
            return CompletableFuture.supplyAsync(() ->
                JvmProbe.getInstance().jvmInfo().mem.getHeapUsedPercent() < threshold()
                    ? Result.healthy()
                    : Result.unhealthy(String.format("Memory percent is over the threshold of %d %%", threshold()))
            );
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(Result.unhealthy(ex));
        }
    }

    private int threshold() {
        return configuration.getProperty("services.health.threshold.memory", Integer.class, 80);
    }
}
