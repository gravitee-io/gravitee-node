/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.opentelemetry.query.tempo;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.configuration.TracingQueryConfiguration;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;

/**
 * Wires a {@link TempoTracingQueryService} from a {@link TracingQueryConfiguration}. Callers can hold their own
 * configuration (typically built via {@link io.gravitee.node.opentelemetry.configuration.TracingQueryConfigurationProvider})
 * and ask the factory for a fully wired service without re-implementing the Vert.x HTTP client glue.
 *
 * @author GraviteeSource Team
 */
public final class TempoTracingQueryServiceFactory {

    private static final String CLIENT_NAME = "opentelemetry-tracing-query";

    private TempoTracingQueryServiceFactory() {
        // no instances
    }

    public static TempoTracingQueryService create(
        final Vertx vertx,
        final Configuration nodeConfiguration,
        final TracingQueryConfiguration configuration
    ) {
        HttpClient httpClient = VertxHttpClientFactory
            .builder()
            .vertx(vertx)
            .nodeConfiguration(nodeConfiguration)
            .defaultTarget(configuration.getUrl())
            .name(CLIENT_NAME)
            .sslOptions(configuration.getSslOptions())
            .proxyOptions(configuration.getProxyOptions())
            .httpOptions(configuration.getHttpOptions())
            .build()
            .createHttpClient();
        return new TempoTracingQueryService(new TempoHttpClient(httpClient, configuration.getHeaders()));
    }
}
