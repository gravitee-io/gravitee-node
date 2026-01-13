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

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.gravitee.node.management.http.utils.SafeBufferedWriter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import lombok.CustomLog;

/**
 * Endpoint used to expose metrics using a Prometheus registry
 * Note that the endpoint is only registered if services.metrics.enabled=true and
 * services.metrics.prometheus.enabled=true.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class PrometheusEndpoint implements ManagementEndpoint {

    public static final String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    private final PrometheusMeterRegistry prometheusRegistry;

    public PrometheusEndpoint() {
        Optional<CompositeMeterRegistry> compositeMeterRegistry = Optional.ofNullable(
            (CompositeMeterRegistry) BackendRegistries.getDefaultNow()
        );

        this.prometheusRegistry =
            (PrometheusMeterRegistry) compositeMeterRegistry
                .flatMap(c ->
                    c.getRegistries().stream().filter(meterRegistry -> meterRegistry instanceof PrometheusMeterRegistry).findFirst()
                )
                .orElse(null);
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
        HttpServerResponse response = routingContext.response();

        response.putHeader(CONTENT_TYPE, CONTENT_TYPE_004);
        response.setChunked(true);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            prometheusRegistry.scrape(baos);
            response.write(Buffer.buffer(baos.toByteArray()));
            if (!response.ended()) {
                response.end();
            }
        } catch (IOException ioe) {
            log.error("Unexpected error while scraping the Prometheus endpoint", ioe);
            if (!response.ended()) {
                routingContext.request().connection().close();
            }
        }
        //        try (BufferedWriter writer = new BufferedWriter(new SafeBufferedWriter(response))) {
        //            prometheusRegistry.scrape(writer);
        //            writer.flush();
        //            if (!response.ended()) {
        //                response.end();
        //            }
        //        } catch (IOException ioe) {
        //            LOGGER.error("Unexpected error while scraping the Prometheus endpoint", ioe);
        //            if (!response.ended()) {
        //                routingContext.request().connection().close();
        //            }
        //        }
    }
}
