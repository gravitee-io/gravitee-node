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

import io.gravitee.common.util.EnvironmentUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@AllArgsConstructor
@Getter
@Setter
public class OpenTelemetryConfiguration {

    private final ConfigurableEnvironment environment;

    @Autowired
    public OpenTelemetryConfiguration(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Value("${services.opentelemetry.enabled:${services.tracing.otel.traces.enabled:true}}")
    private boolean tracesEnabled = true;

    /**
     * Sets the OTLP endpoint to send telemetry data. If unset, defaults to <code>http://localhost:4317</code>.
     * <p>
     * If protocol is `http/protobuf` the version and signal will be appended to the path (e.g. v1/traces or v1/metrics)
     * and the default port will be <code>http://localhost:4318</code>.
     */
    @Value("${services.opentelemetry.exporter.endpoint:${services.tracing.otel.url:http://localhost:4317}}")
    private String endpoint;

    /**
     * Key-value pairs to be used as headers associated with exporter requests.
     */
    @Getter(AccessLevel.NONE)
    private Map<String, String> customHeaders;

    public Map<String, String> getCustomHeaders() {
        if (customHeaders == null) {
            customHeaders = getPropertyMap("services.opentelemetry.exporter.headers");

            if (customHeaders.isEmpty()) {
                customHeaders = getPropertyMap("services.tracing.otel.headers");
            }
        }
        return customHeaders;
    }

    /**
     * Sets the method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include `gzip` and `none`.
     */
    @Value("${services.opentelemetry.exporter.compression:${services.tracing.otel.compression:none}}")
    @Getter(AccessLevel.NONE)
    private String compression;

    public CompressionType getCompressionType() {
        return CompressionType.fromValue(compression);
    }

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of telemetry data. If
     * unset, defaults to 10s.
     */
    @Value("${services.opentelemetry.exporter.timeout:${services.tracing.otel.timeout:10_000}")
    @Getter(AccessLevel.NONE)
    private int timeout;

    public Duration getTimeout() {
        return Duration.ofMillis(timeout);
    }

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the
     * server. Depending on the exporter, the available protocols will be different.
     * <p>
     * Currently, only {@code grpc} and {@code http/protobuf} are allowed.
     * <p>
     * Please mind that changing the protocol requires changing the port in the endpoint as well.
     */
    @Value("${services.opentelemetry.exporter.protocol:${services.tracing.otel.type:grpc}")
    private String protocol;

    public Protocol getProtocol() {
        return Protocol.fromValue(protocol);
    }

    @Value("${services.opentelemetry.exporter.ssl.keystore.type:${services.tracing.otel.ssl.keystore.type:#{null}}")
    private String keystoreType;

    @Value("${services.opentelemetry.exporter.ssl.keystore.path:${services.tracing.otel.ssl.keystore.path:#{null}}")
    private String keystorePath;

    @Value("${services.opentelemetry.exporter.ssl.keystore.password:${services.tracing.otel.ssl.keystore.password:#{null}}")
    private String keystorePassword;

    private List<String> keystorePemCerts;

    public List<String> getKeystorePemCerts() {
        if (keystorePemCerts == null) {
            keystorePemCerts =
                getPropertyList("services.opentelemetry.exporter.keystore.certs", "services.tracing.otel.ssl.keystore.certs");
        }

        return keystorePemCerts;
    }

    private List<String> keystorePemKeys;

    public List<String> getKeystorePemKeys() {
        if (keystorePemKeys == null) {
            keystorePemKeys = getPropertyList("services.opentelemetry.exporter.keystore.keys", "services.tracing.otel.ssl.keystore.keys");
        }

        return keystorePemKeys;
    }

    @Value("${services.opentelemetry.exporter.ssl.trustall:${services.tracing.otel.ssl.trustall:false}")
    private boolean trustAll = false;

    @Value("${services.opentelemetry.exporter.ssl.verifyHost:${services.tracing.otel.ssl.verifyHostname:true}")
    private boolean verifyHost = true;

    @Value("${services.opentelemetry.exporter.ssl.truststore.type:${services.tracing.otel.ssl.truststore.type:#{null}}")
    private String truststoreType;

    @Value("${services.opentelemetry.exporter.ssl.truststore.path:${services.tracing.otel.ssl.truststore.path:#{null}}")
    private String truststorePath;

    @Value("${services.opentelemetry.exporter.ssl.truststore.password:${services.tracing.otel.ssl.truststore.password:#{null}}")
    private String truststorePassword;

    /**
     * If proxy connection must be used.
     */
    @Value("${services.opentelemetry.exporter.proxy.enabled:false}")
    boolean proxyEnabled;

    @Value("${services.opentelemetry.exporter.proxy.useSystemProxy:true}")
    boolean proxyUseSystemProxy;

    @Value("${services.opentelemetry.exporter.proxy.host:#{null}}")
    String proxyHost;

    @Value("${services.opentelemetry.exporter.proxy.port:#{null}}")
    Integer proxyPort;

    @Value("${services.opentelemetry.exporter.proxy.username:#{null}}")
    String proxyUsername;

    @Value("${services.opentelemetry.exporter.proxy.password:#{null}}")
    String proxyPassword;

    private Map<String, String> getPropertyMap(final String key) {
        return EnvironmentUtils
            .getPropertiesStartingWith(environment, key)
            .entrySet()
            .stream()
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(Collectors.toMap(entry -> entry.getKey().substring(key.length() + 1), entry -> String.valueOf(entry.getValue())));
    }

    private List<String> getPropertyList(final String key, final String fallbackKey) {
        List<String> values = new ArrayList<>();
        String indexKey = ("%s[%s]").formatted(key, 0);
        String indexFallBackKey = ("%s[%s]").formatted(fallbackKey, 0);
        while (containsProperty(indexKey, indexFallBackKey)) {
            String value = getProperty(indexKey, indexFallBackKey);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
            indexKey = ("%s[%s]").formatted(key, values.size());
            indexFallBackKey = ("%s[%s]").formatted(fallbackKey, values.size());
        }

        return values;
    }

    private boolean containsProperty(final String key, final String fallbackKey) {
        return environment.containsProperty(key) || environment.containsProperty(fallbackKey);
    }

    private String getProperty(final String key, final String fallbackKey) {
        String value = environment.getProperty(key);
        if (value == null && environment.containsProperty(fallbackKey)) {
            value = environment.getProperty(fallbackKey);
        }
        return value;
    }
}
