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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.internal.AttributesMap;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;

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

    @Value("${services.opentelemetry.enabled:${services.tracing.enabled:false}}")
    private boolean tracesEnabled = false;

    @Value("${services.opentelemetry.verbose:false}")
    private boolean verboseEnabled = false;

    /**
     * Key-value pairs to be used as extra resources attributes associated to any span
     */
    @Getter(AccessLevel.NONE)
    private AttributesMap extraAttributes;

    public AttributesMap getExtraAttributes() {
        if (extraAttributes == null) {
            Map<String, String> configMap = getKeyValuePairs("services.opentelemetry.extraAttributes");
            extraAttributes = AttributesMap.create(configMap.size(), Integer.MAX_VALUE);
            configMap.forEach((k, v) -> extraAttributes.put(AttributeKey.stringKey(k), v));
        }
        return extraAttributes;
    }

    /**
     * Sets the OTLP endpoint to send telemetry data. If unset, defaults to <code>http://localhost:4317</code>.
     * <p>
     * If protocol is `http/protobuf` the version and signal will be appended to the path (e.g. v1/traces or v1/metrics)
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
            customHeaders = getKeyValuePairs("services.opentelemetry.exporter.headers");

            if (customHeaders.isEmpty()) {
                customHeaders = getKeyValuePairs("services.tracing.otel.headers");
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
    @Value("${services.opentelemetry.exporter.timeout:${services.tracing.otel.timeout:10000}}")
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
    @Value("${services.opentelemetry.exporter.protocol:${services.tracing.otel.type:grpc}}")
    private String protocol;

    public Protocol getProtocol() {
        return Protocol.fromValue(protocol);
    }

    @Value("${services.opentelemetry.exporter.ssl.keystore.type:${services.tracing.otel.ssl.keystore.type:#{null}}}")
    private String keystoreType;

    @Value("${services.opentelemetry.exporter.ssl.keystore.path:${services.tracing.otel.ssl.keystore.path:#{null}}}")
    private String keystorePath;

    @Value("${services.opentelemetry.exporter.ssl.keystore.password:${services.tracing.otel.ssl.keystore.password:#{null}}}")
    private String keystorePassword;

    private List<String> keystorePemCerts;

    public List<String> getKeystorePemCerts() {
        if (keystorePemCerts == null) {
            keystorePemCerts =
                getPropertyList("services.opentelemetry.exporter.ssl.keystore.certs", "services.tracing.otel.ssl.keystore.certs");
        }

        return keystorePemCerts;
    }

    private List<String> keystorePemKeys;

    public List<String> getKeystorePemKeys() {
        if (keystorePemKeys == null) {
            keystorePemKeys =
                getPropertyList("services.opentelemetry.exporter.ssl.keystore.keys", "services.tracing.otel.ssl.keystore.keys");
        }

        return keystorePemKeys;
    }

    @Value("${services.opentelemetry.exporter.ssl.trustAll:${services.tracing.otel.ssl.trustall:false}}")
    private boolean trustAll = false;

    @Value("${services.opentelemetry.exporter.ssl.verifyHost:${services.tracing.otel.ssl.verifyHostname:true}}")
    private boolean verifyHost = true;

    @Value("${services.opentelemetry.exporter.ssl.truststore.type:${services.tracing.otel.ssl.truststore.type:#{null}}}")
    private String truststoreType;

    @Value("${services.opentelemetry.exporter.ssl.truststore.path:${services.tracing.otel.ssl.truststore.path:#{null}}}")
    private String truststorePath;

    @Value("${services.opentelemetry.exporter.ssl.truststore.password:${services.tracing.otel.ssl.truststore.password:#{null}}}")
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

    private List<String> getPropertyList(final String key, final String fallbackKey) {
        Map<String, Object> properties = EnvironmentUtils.getPropertiesStartingWith(environment, key);
        if (properties.isEmpty()) {
            properties = EnvironmentUtils.getPropertiesStartingWith(environment, fallbackKey);
            return toList(properties, fallbackKey);
        }
        return toList(properties, key);
    }

    private List<String> toList(Map<String, Object> elements, String baseKey) {
        return IntStream
            .range(0, elements.size())
            .boxed()
            .map(i -> baseKey.concat("[%d]".formatted(i)))
            .map(k -> elements.get(k).toString())
            .filter(s -> !s.isBlank())
            .toList();
    }

    private Map<String, String> getKeyValuePairs(String baseKey) {
        Map<String, String> properties = new HashMap<>();
        getPropertiesStartingWith(baseKey)
            .forEach(entry -> {
                // keep what is after '].'
                int end = entry.getKey().lastIndexOf("].");
                if (end > 0) {
                    properties.put(entry.getKey().substring(end + 2), entry.getValue().toString());
                }
            });
        return properties;
    }

    private Stream<Map.Entry<String, Object>> getPropertiesStartingWith(final String key) {
        return EnvironmentUtils
            .getPropertiesStartingWith(environment, key)
            .entrySet()
            .stream()
            .filter(entry -> Objects.nonNull(entry.getValue()));
    }
}
