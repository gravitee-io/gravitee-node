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
package io.gravitee.node.management.http.metrics.prometheus;

import static io.prometheus.client.exporter.common.TextFormat.*;
import static io.vertx.core.http.HttpHeaders.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.gravitee.node.management.http.utils.SafeBufferedWriter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PrometheusEndpoint implements ManagementEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusEndpoint.class);

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/metrics/prometheus";
    }

    @Override
    public void handle(RoutingContext routingContext) {
        PrometheusMeterRegistry registry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
        HttpServerResponse response = routingContext.response();

        response.putHeader(CONTENT_TYPE, CONTENT_TYPE_004);
        response.setChunked(true);

        try (BufferedWriter writer = new BufferedWriter(new SafeBufferedWriter(response))) {
            registry.scrape(writer);
        } catch (IOException ioe) {
            LOGGER.error("Unexpected error while scraping the Prometheus endpoint", ioe);
            response.close();
        }
    }
}
