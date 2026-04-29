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
import io.gravitee.node.vertx.client.http.VertxHttpProxyType;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.Environment;

/**
 * Builds a {@link TracingQueryConfiguration} by reading keys under a caller-chosen property prefix from a Spring
 * {@link Environment}. Mirrors the pattern used by the Redis cache plugin's {@code RedisConfigurationProvider} so any caller
 * can plug the tracing query service into its own configuration namespace by passing the matching prefix.
 *
 * <p>Expected key layout (relative to {@code prefix}):
 * <pre>
 * {prefix}.url                          : String,  default "http://localhost:3200"
 * {prefix}.headers[N].name              : String   (repeatable, indexed from 0)
 * {prefix}.headers[N].value             : String
 * {prefix}.ssl.trustAll                 : Boolean, default false
 * {prefix}.ssl.verifyHost               : Boolean, default true
 * {prefix}.proxy.enabled                : Boolean, default false
 * {prefix}.proxy.useSystemProxy         : Boolean, default false
 * {prefix}.proxy.host                   : String
 * {prefix}.proxy.port                   : Integer, default 0
 * {prefix}.proxy.username               : String
 * {prefix}.proxy.password               : String
 * {prefix}.proxy.type                   : VertxHttpProxyType (HTTP|SOCKS4|SOCKS5), default HTTP
 * {prefix}.http.connectTimeout          : Long,    default 5000
 * {prefix}.http.idleTimeout             : Long,    default 60000
 * </pre>
 *
 * @author GraviteeSource Team
 */
public final class TracingQueryConfigurationProvider {

    private TracingQueryConfigurationProvider() {
        // no instances
    }

    public static TracingQueryConfiguration from(final Environment environment, final String prefix) {
        TracingQueryConfiguration configuration = new TracingQueryConfiguration();
        configuration.setUrl(environment.getProperty(prefix + ".url", String.class, configuration.getUrl()));
        configuration.setHeaders(readHeaders(environment, prefix));
        configuration.setSslOptions(readSslOptions(environment, prefix));
        configuration.setProxyOptions(readProxyOptions(environment, prefix));
        configuration.setHttpOptions(readHttpOptions(environment, prefix));
        return configuration;
    }

    private static Map<String, String> readHeaders(final Environment environment, final String prefix) {
        Map<String, String> headers = new HashMap<>();
        for (int index = 0;; index++) {
            String name = environment.getProperty(prefix + ".headers[" + index + "].name", String.class);
            if (name == null) {
                break;
            }
            String value = environment.getProperty(prefix + ".headers[" + index + "].value", String.class, "");
            headers.put(name, value);
        }
        return headers;
    }

    private static SslOptions readSslOptions(final Environment environment, final String prefix) {
        return SslOptions
            .builder()
            .trustAll(environment.getProperty(prefix + ".ssl.trustAll", Boolean.class, false))
            .hostnameVerifier(environment.getProperty(prefix + ".ssl.verifyHost", Boolean.class, true))
            .build();
    }

    private static VertxHttpProxyOptions readProxyOptions(final Environment environment, final String prefix) {
        if (!environment.getProperty(prefix + ".proxy.enabled", Boolean.class, false)) {
            return null;
        }
        return VertxHttpProxyOptions
            .builder()
            .enabled(true)
            .useSystemProxy(environment.getProperty(prefix + ".proxy.useSystemProxy", Boolean.class, false))
            .host(environment.getProperty(prefix + ".proxy.host", String.class))
            .port(environment.getProperty(prefix + ".proxy.port", Integer.class, 0))
            .username(environment.getProperty(prefix + ".proxy.username", String.class))
            .password(environment.getProperty(prefix + ".proxy.password", String.class))
            .type(environment.getProperty(prefix + ".proxy.type", VertxHttpProxyType.class, VertxHttpProxyType.HTTP))
            .build();
    }

    private static VertxHttpClientOptions readHttpOptions(final Environment environment, final String prefix) {
        return VertxHttpClientOptions
            .builder()
            .connectTimeout(environment.getProperty(prefix + ".http.connectTimeout", Long.class, 5000L))
            .idleTimeout(environment.getProperty(prefix + ".http.idleTimeout", Long.class, 60000L))
            .build();
    }
}
