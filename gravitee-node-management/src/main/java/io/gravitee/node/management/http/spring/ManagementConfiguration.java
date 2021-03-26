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
package io.gravitee.node.management.http.spring;

import io.gravitee.node.api.healthcheck.ProbeManager;
import io.gravitee.node.management.healthcheck.ProbeManagerImpl;
import io.gravitee.node.management.http.ManagementService;
import io.gravitee.node.management.http.configuration.ConfigurationEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.management.http.metrics.prometheus.PrometheusEndpoint;
import io.gravitee.node.management.http.node.NodeEndpoint;
import io.gravitee.node.management.http.vertx.endpoint.ManagementEndpointManagerImpl;
import io.gravitee.node.management.http.vertx.spring.HttpServerSpringConfiguration;
import io.gravitee.node.management.http.vertx.verticle.ManagementVerticle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import(HttpServerSpringConfiguration.class)
public class ManagementConfiguration {

    @Bean
    public ManagementService managementService() {
        return new ManagementService();
    }

    @Bean
    public ManagementVerticle managementVerticle() {
        return new ManagementVerticle();
    }

    @Bean
    public ManagementEndpointManager managementEndpointManager() {
        return new ManagementEndpointManagerImpl();
    }

    @Bean
    public NodeEndpoint nodeEndpoint() {
        return new NodeEndpoint();
    }

    @Bean
    public PrometheusEndpoint prometheusEndpoint() {
        return new PrometheusEndpoint();
    }

    @Bean
    public ConfigurationEndpoint configurationEndpoint() {
        return new ConfigurationEndpoint();
    }

    @Bean
    public ProbeManager probeManager() {
        return new ProbeManagerImpl();
    }
}
