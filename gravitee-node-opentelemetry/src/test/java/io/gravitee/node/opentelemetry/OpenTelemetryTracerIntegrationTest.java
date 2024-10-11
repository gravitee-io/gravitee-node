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
package io.gravitee.node.opentelemetry;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.node.opentelemetry.configuration.Protocol;
import io.gravitee.node.opentelemetry.exporter.ExporterFactory;
import io.gravitee.node.opentelemetry.testcontainers.JaegerAllInOne;
import io.gravitee.node.opentelemetry.tracer.instrumentation.internal.InternalInstrumenterTracerFactory;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.VertxHttpInstrumenterTracerFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.BindMode;

@ExtendWith(value = { MockitoExtension.class, VertxExtension.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class OpenTelemetryTracerIntegrationTest {

    private static final String JAEGER_DOCKER_IMAGE = "jaegertracing/all-in-one:1.62.0";
    private static final JaegerAllInOne container = new JaegerAllInOne(JAEGER_DOCKER_IMAGE);
    private static final JaegerAllInOne containerTLS = new JaegerAllInOne(JAEGER_DOCKER_IMAGE)
        .withClasspathResourceMapping("ssl/ca.pem", "/certs/ca.pem", BindMode.READ_WRITE)
        .withClasspathResourceMapping("ssl/server.jaeger.crt", "/certs/server.jaeger.crt", BindMode.READ_WRITE)
        .withClasspathResourceMapping("ssl/server.jaeger.key", "/certs/server.jaeger.key", BindMode.READ_WRITE)
        .withClasspathResourceMapping("ssl/client.cer", "/certs/client.cer", BindMode.READ_WRITE)
        .withClasspathResourceMapping("ssl/client.key", "/certs/client.key", BindMode.READ_WRITE)
        .withEnv(
            Map.ofEntries(
                Map.entry("COLLECTOR_OTLP_GRPC_TLS_ENABLED", "true"),
                Map.entry("COLLECTOR_OTLP_GRPC_TLS_CLIENT_CA", "/certs/ca.pem"),
                Map.entry("COLLECTOR_OTLP_GRPC_TLS_CERT", "/certs/server.jaeger.crt"),
                Map.entry("COLLECTOR_OTLP_GRPC_TLS_KEY", "/certs/server.jaeger.key"),
                Map.entry("COLLECTOR_OTLP_HTTP_TLS_ENABLED", "true"),
                Map.entry("COLLECTOR_OTLP_HTTP_TLS_CLIENT_CA", "/certs/ca.pem"),
                Map.entry("COLLECTOR_OTLP_HTTP_TLS_CERT", "/certs/server.jaeger.crt"),
                Map.entry("COLLECTOR_OTLP_HTTP_TLS_KEY", "/certs/server.jaeger.key")
            )
        );

    @BeforeAll
    void beforeAll() {
        container.start();
        containerTLS.start();
    }

    @AfterAll
    void afterAll() {
        container.stop();
        containerTLS.stop();
    }

    @BeforeEach
    void beforeEach() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void should_connect_to_jaeger_over_grpc(Vertx vertx) throws Exception {
        var openTelemetryConfiguration = OpenTelemetryConfiguration
            .builder()
            .endpoint("http://localhost:" + container.getCollectorGrpcPort())
            .tracesEnabled(true)
            .protocol(Protocol.GRPC.value())
            .environment(new MockEnvironment())
            .build();

        var serviceName = "jaeger_grpc_unsecured";

        OpenTelemetryFactory openTelemetryFactory = openTelemetryFactory(vertx, openTelemetryConfiguration);
        var tracer = openTelemetryFactory.createTracer(
            "serviceInstanceId",
            serviceName,
            "serviceNamespace",
            "serviceVersion",
            List.of(new VertxHttpInstrumenterTracerFactory(), new InternalInstrumenterTracerFactory())
        );
        tracer.start();

        Context vertxContext = vertx.getOrCreateContext();
        Context duplicatedContext = VertxContext.createNewDuplicatedContext(vertxContext);
        duplicatedContext.runOnContext(v -> {
            var span = tracer.startSpanFrom(duplicatedContext, new InternalRequest("my-span", Map.of("custom", "value")));
            tracer.end(duplicatedContext, span);
        });

        await()
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                var client = container.client(vertx);
                var response = client
                    .get("/api/traces")
                    .addQueryParam("service", serviceName)
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get();

                assertThat(response.statusCode()).isEqualTo(200);
                assertData(response.bodyAsJsonObject());
            });
    }

    @NonNull
    private static OpenTelemetryFactory openTelemetryFactory(
        final Vertx vertx,
        final OpenTelemetryConfiguration openTelemetryConfiguration
    ) {
        return new OpenTelemetryFactory(openTelemetryConfiguration, new ExporterFactory(openTelemetryConfiguration, vertx));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generator")
    void should_connect_to_a_secure_jaeger_over_grpc(String name, JsonObject sslConfig, Vertx vertx) throws Exception {
        var keyStore = sslConfig.getJsonObject("keyStore");
        var trustStore = sslConfig.getJsonObject("trustStore");

        var environment = new MockEnvironment();
        if (keyStore.getString("type").equals("PEM")) {
            environment.withProperty("services.tracing.otel.ssl.keystore.certs[0]", keyStore.getString("cert"));
            environment.withProperty("services.tracing.otel.ssl.keystore.keys[0]", keyStore.getString("key"));
        }

        var openTelemetryConfiguration = OpenTelemetryConfiguration
            .builder()
            .environment(environment)
            .keystoreType(keyStore.getString("type"))
            .keystorePath(keyStore.getString("path"))
            .keystorePassword(keyStore.getString("password"))
            .truststoreType(trustStore.getString("type"))
            .truststorePath(trustStore.getString("path"))
            .truststorePassword(trustStore.getString("password"))
            .endpoint("https://localhost:" + containerTLS.getCollectorGrpcPort())
            .tracesEnabled(true)
            .protocol(Protocol.GRPC.value())
            .build();

        var serviceName = "otel_grpc_" + keyStore.getString("type");
        OpenTelemetryFactory openTelemetryFactory = openTelemetryFactory(vertx, openTelemetryConfiguration);

        var tracer = openTelemetryFactory.createTracer(
            "serviceInstanceId",
            serviceName,
            "serviceNamespace",
            "serviceVersion",
            List.of(new VertxHttpInstrumenterTracerFactory(), new InternalInstrumenterTracerFactory())
        );
        tracer.start();

        Context vertxContext = vertx.getOrCreateContext();
        Context duplicatedContext = VertxContext.createNewDuplicatedContext(vertxContext);
        duplicatedContext.runOnContext(v -> {
            var span = tracer.startSpanFrom(duplicatedContext, new InternalRequest("my-span", Map.of("custom", "value")));
            tracer.end(duplicatedContext, span);
        });

        await()
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                var client = containerTLS.client(vertx);
                var response = client
                    .get("/api/traces")
                    .addQueryParam("service", serviceName)
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get();

                assertThat(response.statusCode()).isEqualTo(200);
                assertData(response.bodyAsJsonObject());
            });
    }

    @Test
    void should_connect_to_jaeger_over_http(Vertx vertx) throws Exception {
        var openTelemetryConfiguration = OpenTelemetryConfiguration
            .builder()
            .endpoint("http://localhost:" + container.getCollectorHttpPort())
            .tracesEnabled(true)
            .protocol(Protocol.HTTP_PROTOBUF.value())
            .environment(new MockEnvironment())
            .build();

        var serviceName = "jaeger_http_unsecured";
        OpenTelemetryFactory openTelemetryFactory = openTelemetryFactory(vertx, openTelemetryConfiguration);
        var tracer = openTelemetryFactory.createTracer(
            "serviceInstanceId",
            serviceName,
            "serviceNamespace",
            "serviceVersion",
            List.of(new VertxHttpInstrumenterTracerFactory(), new InternalInstrumenterTracerFactory())
        );
        tracer.start();

        Context vertxContext = vertx.getOrCreateContext();
        Context duplicatedContext = VertxContext.createNewDuplicatedContext(vertxContext);
        duplicatedContext.runOnContext(v -> {
            var span = tracer.startSpanFrom(duplicatedContext, new InternalRequest("my-span", Map.of("custom", "value")));
            tracer.end(duplicatedContext, span);
        });

        await()
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                var client = container.client(vertx);
                var response = client
                    .get("/api/traces")
                    .addQueryParam("service", serviceName)
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get();

                assertThat(response.statusCode()).isEqualTo(200);
                assertData(response.bodyAsJsonObject());
            });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generator")
    void should_connect_to_a_secure_jaeger_over_http(String name, JsonObject sslConfig, Vertx vertx) throws Exception {
        var keyStore = sslConfig.getJsonObject("keyStore");
        var trustStore = sslConfig.getJsonObject("trustStore");

        var environment = new MockEnvironment();
        if (keyStore.getString("type").equals("PEM")) {
            environment.withProperty("services.tracing.otel.ssl.keystore.certs[0]", keyStore.getString("cert"));
            environment.withProperty("services.tracing.otel.ssl.keystore.keys[0]", keyStore.getString("key"));
        }

        var openTelemetryConfiguration = OpenTelemetryConfiguration
            .builder()
            .environment(environment)
            .keystoreType(keyStore.getString("type"))
            .keystorePath(keyStore.getString("path"))
            .keystorePassword(keyStore.getString("password"))
            .truststoreType(trustStore.getString("type"))
            .truststorePath(trustStore.getString("path"))
            .truststorePassword(trustStore.getString("password"))
            .endpoint("https://localhost:" + containerTLS.getCollectorHttpPort())
            .tracesEnabled(true)
            .protocol(Protocol.HTTP_PROTOBUF.value())
            .build();

        var serviceName = "otel_http_" + keyStore.getString("type");
        OpenTelemetryFactory openTelemetryFactory = openTelemetryFactory(vertx, openTelemetryConfiguration);
        var tracer = openTelemetryFactory.createTracer(
            "serviceInstanceId",
            serviceName,
            "serviceNamespace",
            "serviceVersion",
            List.of(new VertxHttpInstrumenterTracerFactory(), new InternalInstrumenterTracerFactory())
        );
        tracer.start();

        Context vertxContext = vertx.getOrCreateContext();
        Context duplicatedContext = VertxContext.createNewDuplicatedContext(vertxContext);
        duplicatedContext.runOnContext(v -> {
            var span = tracer.startSpanFrom(duplicatedContext, new InternalRequest("my-span", Map.of("custom", "value")));
            tracer.end(duplicatedContext, span);
        });

        await()
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                var client = containerTLS.client(vertx);
                var response = client
                    .get("/api/traces")
                    .addQueryParam("service", serviceName)
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get();

                assertThat(response.statusCode()).isEqualTo(200);
                assertData(response.bodyAsJsonObject());
            });
    }

    private static Stream<Arguments> generator() {
        return Stream.of(
            Arguments.of(
                "using a jks file",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry(
                            "keyStore",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry("type", "JKS"),
                                    Map.entry("path", "src/test/resources/ssl/client-keystore.jks"),
                                    Map.entry("password", "gravitee"),
                                    Map.entry("alias", "jaeger-client")
                                )
                            )
                        ),
                        Map.entry(
                            "trustStore",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry("type", "JKS"),
                                    Map.entry("path", "src/test/resources/ssl/client-truststore.jks"),
                                    Map.entry("password", "gravitee")
                                )
                            )
                        )
                    )
                )
            ),
            Arguments.of(
                "using a PKCS12 file",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry(
                            "keyStore",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry("type", "PKCS12"),
                                    Map.entry("path", "src/test/resources/ssl/client-keystore.p12"),
                                    Map.entry("password", "gravitee"),
                                    Map.entry("alias", "jaeger-client")
                                )
                            )
                        ),
                        Map.entry(
                            "trustStore",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry("type", "PKCS12"),
                                    Map.entry("path", "src/test/resources/ssl/client-truststore.p12"),
                                    Map.entry("password", "gravitee")
                                )
                            )
                        )
                    )
                )
            ),
            Arguments.of(
                "using a PEM file",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry(
                            "keyStore",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry("type", "PEM"),
                                    Map.entry("cert", "src/test/resources/ssl/client.cer"),
                                    Map.entry("key", "src/test/resources/ssl/client.key")
                                )
                            )
                        ),
                        Map.entry(
                            "trustStore",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry("type", "PEM"),
                                    Map.entry("path", "src/test/resources/ssl/client-truststore.pem"),
                                    Map.entry("password", "gravitee")
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    private void assertData(JsonObject json) {
        var data = json.getJsonArray("data");
        assertThat(data).isNotEmpty();

        var trace = data.getJsonObject(0);
        var spans = trace.getJsonArray("spans");
        assertThat(spans).isNotEmpty();

        var span = spans.getJsonObject(0);
        assertThat(span.getString("operationName")).isEqualTo("my-span");
        JsonArray tags = span.getJsonArray("tags");
        JsonObject customTags = tags.getJsonObject(0);
        assertThat(customTags.getString("key")).isEqualTo("custom");
        assertThat(customTags.getString("value")).isEqualTo("value");
        JsonObject spanKind = tags.getJsonObject(3);
        assertThat(spanKind.getString("key")).isEqualTo("span.kind");
        assertThat(spanKind.getString("value")).isEqualTo("internal");
    }
}
