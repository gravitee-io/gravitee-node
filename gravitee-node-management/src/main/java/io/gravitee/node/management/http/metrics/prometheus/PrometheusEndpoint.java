/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.management.http.metrics.prometheus;

import static io.prometheus.client.exporter.common.TextFormat.CONTENT_TYPE_004;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.gravitee.node.management.http.utils.SafeBufferedWriter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;
import java.io.BufferedWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PrometheusEndpoint implements ManagementEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusEndpoint.class);

    private PrometheusMeterRegistry prometheusRegistry = null;

    public PrometheusEndpoint(boolean prometheusEnabled) {
        if (prometheusEnabled) {
            this.prometheusRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
        }
    }

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
        if (prometheusRegistry == null) {
            routingContext.response().setStatusCode(501).end("Prometheus metrics are not enabled");
            return;
        }

        HttpServerResponse response = routingContext.response();

        response.putHeader(CONTENT_TYPE, CONTENT_TYPE_004);
        response.setChunked(true);

        try (
            SafeBufferedWriter safeBufferedWriter = new SafeBufferedWriter(response);
            BufferedWriter writer = new BufferedWriter(safeBufferedWriter)
        ) {
            prometheusRegistry.scrape(writer);
            writer.flush();
        } catch (IOException ioe) {
            // On write-queue drain timeout, abort the TCP connection so the client
            // gets a clean error instead of hanging. close() fires the closeHandler
            // in ConcurrencyLimitHandler which releases the semaphore permit.
            LOGGER.error("Unexpected error while scraping the Prometheus endpoint", ioe);
            if (!response.ended() && !response.closed()) {
                response.close();
            }
        } finally {
            // Ensure the response is always terminated. The closed() guard prevents
            // calling end() on an already-aborted connection (which would throw in
            // some Vert.x versions).
            if (!response.ended() && !response.closed()) {
                response.end();
            }
        }
    }
}
