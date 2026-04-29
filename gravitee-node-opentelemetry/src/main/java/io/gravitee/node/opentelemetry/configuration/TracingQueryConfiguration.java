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
package io.gravitee.node.opentelemetry.configuration;

import io.gravitee.node.vertx.client.http.VertxHttpClientOptions;
import io.gravitee.node.vertx.client.http.VertxHttpProxyOptions;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * POJO holding the connection settings for a tracing query backend (currently Tempo). Construct via
 * {@link TracingQueryConfigurationProvider#from} when reading from a Spring {@code Environment}, or instantiate directly when
 * a caller already has the values in hand.
 *
 * @author GraviteeSource Team
 */
@Data
public class TracingQueryConfiguration {

    private String url = "http://localhost:3200";
    private Map<String, String> headers = new HashMap<>();
    private SslOptions sslOptions = SslOptions.builder().build();
    private VertxHttpProxyOptions proxyOptions;
    private VertxHttpClientOptions httpOptions = VertxHttpClientOptions.builder().build();
}
