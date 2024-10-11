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
package io.gravitee.node.opentelemetry.exporter;

import com.google.common.base.Strings;
import io.gravitee.node.opentelemetry.configuration.CompressionType;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.node.opentelemetry.configuration.Protocol;
import io.gravitee.node.opentelemetry.exporter.sender.VertxGrpcSender;
import io.gravitee.node.opentelemetry.exporter.sender.VertxHttpSender;
import io.gravitee.node.opentelemetry.exporter.tracing.VertxGrpcSpanExporter;
import io.gravitee.node.opentelemetry.exporter.tracing.VertxHttpSpanExporter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ProxyOptions;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 * @see <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/exporter/otlp/OTelExporterRecorder.java">OTelExporterRecorder.java</a>
 */
@RequiredArgsConstructor
@Slf4j
public class ExporterFactory {

    private static final String OTLP_VALUE = "otlp";
    private final OpenTelemetryConfiguration openTelemetryConfiguration;
    private final Vertx vertx;

    public SpanExporter createSpanExporter() {
        URI tracesUri = getTracesUri();
        Protocol protocol = getProtocol();
        if (protocol == Protocol.HTTP_PROTOBUF || protocol == Protocol.HTTP) {
            return createHttpSpanExporter(tracesUri, protocol);
        } else {
            return createGrpcSpanExporter(tracesUri);
        }
    }

    private SpanExporter createGrpcSpanExporter(final URI tracesUri) {
        return new VertxGrpcSpanExporter(
            new GrpcExporter<TraceRequestMarshaler>(
                OTLP_VALUE, // use the same as OTel does
                "span", // use the same as OTel does
                new VertxGrpcSender(
                    tracesUri,
                    VertxGrpcSender.GRPC_TRACE_SERVICE_NAME,
                    determineCompression(),
                    openTelemetryConfiguration.getTimeout(),
                    populateTracingExportHttpHeaders(),
                    new HttpClientOptionsConsumer(openTelemetryConfiguration, tracesUri),
                    vertx
                ),
                MeterProvider::noop
            )
        );
    }

    private SpanExporter createHttpSpanExporter(URI baseUri, final Protocol protocol) {
        boolean exportAsJson = protocol == Protocol.HTTP;

        return new VertxHttpSpanExporter(
            new HttpExporter<>(
                OTLP_VALUE, // use the same as OTel does
                "span", // use the same as OTel does
                new VertxHttpSender(
                    baseUri,
                    VertxHttpSender.TRACES_PATH,
                    determineCompression(),
                    openTelemetryConfiguration.getTimeout(),
                    populateTracingExportHttpHeaders(),
                    exportAsJson ? "application/json" : "application/x-protobuf",
                    new HttpClientOptionsConsumer(openTelemetryConfiguration, baseUri),
                    vertx
                ),
                MeterProvider::noop,
                exportAsJson
            )
        );
    }

    private Protocol getProtocol() {
        Protocol protocol = openTelemetryConfiguration.getProtocol();
        if (Protocol.UNKNOWN == protocol) {
            log.warn("OpenTelemetry protocol unsupported, fallback to GRPC.");
            protocol = Protocol.GRPC;
        }
        return protocol;
    }

    private URI getTracesUri() {
        String endpoint = openTelemetryConfiguration.getEndpoint();
        // Replace grpc to http to handle backward compatibilty
        if (endpoint.startsWith("grpc://")) {
            endpoint = endpoint.replace("grpc://", "http://");
        } else if (endpoint.startsWith("grpcs://")) {
            endpoint = endpoint.replace("grpcs://", "https://");
        }
        return ExporterBuilderUtil.validateEndpoint(endpoint);
    }

    private boolean determineCompression() {
        return openTelemetryConfiguration.getCompressionType() == CompressionType.GZIP;
    }

    private Map<String, String> populateTracingExportHttpHeaders() {
        Map<String, String> headersMap = new HashMap<>();

        if (openTelemetryConfiguration.getCustomHeaders() != null) {
            headersMap.putAll(openTelemetryConfiguration.getCustomHeaders());
        }

        return headersMap;
    }

    private record HttpClientOptionsConsumer(OpenTelemetryConfiguration configuration, URI baseUri) implements Consumer<HttpClientOptions> {
        private static final String KEYSTORE_FORMAT_JKS = "JKS";
        private static final String KEYSTORE_FORMAT_PEM = "PEM";
        private static final String KEYSTORE_FORMAT_PKCS12 = "PKCS12";

        @Override
        public void accept(HttpClientOptions options) {
            if (OTelExporterUtil.isHttps(baseUri)) {
                configureSsl(options);
            }
            if (configuration.isProxyEnabled()) {
                configureProxyOptions(options);
            }
        }

        private void configureSsl(HttpClientOptions options) {
            options.setSsl(true).setUseAlpn(true);

            if (configuration.isTrustAll()) {
                options.setTrustAll(true).setVerifyHost(false);
            } else {
                options.setTrustAll(false).setVerifyHost(configuration.isVerifyHost());
            }

            if (KEYSTORE_FORMAT_JKS.equalsIgnoreCase(configuration.getKeystoreType())) {
                options.setKeyCertOptions(
                    new JksOptions().setPath(configuration.getKeystorePath()).setPassword(configuration.getKeystorePassword())
                );
            } else if (KEYSTORE_FORMAT_PKCS12.equalsIgnoreCase(configuration.getKeystoreType())) {
                options.setKeyCertOptions(
                    new PfxOptions().setPath(configuration.getKeystorePath()).setPassword(configuration.getKeystorePassword())
                );
            } else if (KEYSTORE_FORMAT_PEM.equalsIgnoreCase(configuration.getKeystoreType())) {
                options.setKeyCertOptions(
                    new PemKeyCertOptions()
                        .setCertPaths(configuration.getKeystorePemCerts())
                        .setKeyPaths(configuration.getKeystorePemKeys())
                );
            }

            if (KEYSTORE_FORMAT_JKS.equalsIgnoreCase(configuration.getTruststoreType())) {
                options.setTrustOptions(
                    new JksOptions().setPath(configuration.getTruststorePath()).setPassword(configuration.getTruststorePassword())
                );
            } else if (KEYSTORE_FORMAT_PKCS12.equalsIgnoreCase(configuration.getTruststoreType())) {
                options.setTrustOptions(
                    new PfxOptions().setPath(configuration.getTruststorePath()).setPassword(configuration.getTruststorePassword())
                );
            } else if (KEYSTORE_FORMAT_PEM.equalsIgnoreCase(configuration.getTruststoreType())) {
                options.setTrustOptions(new PemTrustOptions().addCertPath(configuration.getTruststorePath()));
            }
        }

        private void configureProxyOptions(HttpClientOptions options) {
            String proxyHost = configuration.getProxyHost();
            if (!Strings.isNullOrEmpty(proxyHost)) {
                ProxyOptions proxyOptions = new ProxyOptions().setHost(proxyHost);
                if (configuration.getProxyPort() != null) {
                    proxyOptions.setPort(configuration.getProxyPort());
                }
                if (!Strings.isNullOrEmpty(configuration.getProxyUsername())) {
                    proxyOptions.setUsername(configuration.getProxyUsername());
                }
                if (!Strings.isNullOrEmpty(configuration.getProxyPassword())) {
                    proxyOptions.setPassword(configuration.getProxyPassword());
                }
                options.setProxyOptions(proxyOptions);
            } else if (configuration.isProxyUseSystemProxy()) {
                configureProxyOptionsFromJDKSysProps(options);
            }
        }

        private void configureProxyOptionsFromJDKSysProps(HttpClientOptions options) {
            String proxyHost = options.isSsl()
                ? System.getProperty("https.proxyHost", "none")
                : System.getProperty("http.proxyHost", "none");
            String proxyPortAsString = options.isSsl()
                ? System.getProperty("https.proxyPort", "443")
                : System.getProperty("http.proxyPort", "80");
            int proxyPort = Integer.parseInt(proxyPortAsString);

            if (!"none".equals(proxyHost)) {
                ProxyOptions proxyOptions = new ProxyOptions().setHost(proxyHost).setPort(proxyPort);
                String proxyUser = options.isSsl() ? System.getProperty("https.proxyUser") : System.getProperty("http.proxyUser");
                if (proxyUser != null && !proxyUser.isBlank()) {
                    proxyOptions.setUsername(proxyUser);
                }
                String proxyPassword = options.isSsl()
                    ? System.getProperty("https.proxyPassword")
                    : System.getProperty("http.proxyPassword");
                if (proxyPassword != null && !proxyPassword.isBlank()) {
                    proxyOptions.setPassword(proxyPassword);
                }
                options.setProxyOptions(proxyOptions);
            }
        }
    }
}
