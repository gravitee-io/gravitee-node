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
package io.gravitee.node.service.healthcheck.spring;

import io.gravitee.node.service.healthcheck.management.HealthcheckManagementEndpoint;
import io.gravitee.node.service.healthcheck.probe.CPUProbe;
import io.gravitee.node.service.healthcheck.probe.MemoryProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class NodeHealthcheckConfiguration {

    @Bean
    public HealthcheckManagementEndpoint healthcheckManagementEndpoint() {
        return new HealthcheckManagementEndpoint();
    }

    // FIXME: to delete with #5294 https://github.com/gravitee-io/issues/issues/5294
    @Bean
    public CPUProbe cpuProbe(@Value("${services.health.threshold.cpu:80}") int threshold) {
        return new CPUProbe(threshold);
    }

    // FIXME: to delete with #5294 https://github.com/gravitee-io/issues/issues/5294
    @Bean
    public MemoryProbe memoryProbe(@Value("${services.health.threshold.memory:80}") int threshold) {
        return new MemoryProbe(threshold);
    }
}
