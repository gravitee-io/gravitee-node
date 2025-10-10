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
package io.gravitee.node.monitoring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.backends.NoopBackendRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Metrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);

    private Metrics() {}

    /**
     * Get the default micrometer registry.
     * @return the micrometer registry. You can enable metrics by setting
     * services.metrics.enabled=true  inside gravitee.yaml or environmental variable gravitee_services_metrics_enabled=true
     */
    public static MeterRegistry getDefaultRegistry() {
        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if (registry == null) {
            LOGGER.error("Gravitee metrics is disabled. You need to enable it first (services.metrics.enabled=true)");
            return NoopBackendRegistry.INSTANCE.getMeterRegistry();
        }
        return registry;
    }

    /**
     * Get the micrometer registry of the given name. May return null if it hasn't been registered yet or if it has been stopped.
     * <p/>
     * <b>WARN</b>: this method should not be used, default registry should always be used.
     *
     * @param registryName  – the name associated with this registry in Micrometer options
     * @return the micrometer registry or null if metrics is not enabled. You can enable metrics by setting
     * services.metrics.enabled=true  inside gravitee.yaml or environmental variable gravitee_services_metrics_enabled=true
     */
    @Deprecated
    public static MeterRegistry getRegistry(String registryName) {
        MeterRegistry registry = BackendRegistries.getNow(registryName);
        if (registry == null) {
            LOGGER.error("Gravitee metrics is disabled. You need to enable it first (services.metrics.enabled=true)");
            return NoopBackendRegistry.INSTANCE.getMeterRegistry();
        }
        return registry;
    }
}
