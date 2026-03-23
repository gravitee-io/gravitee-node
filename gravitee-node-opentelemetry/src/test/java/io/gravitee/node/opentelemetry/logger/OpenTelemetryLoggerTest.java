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
package io.gravitee.node.opentelemetry.logger;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenTelemetryLoggerTest {

    private InMemoryLogRecordExporter logExporter;
    private OpenTelemetryLogger otelLogger;

    @BeforeEach
    void setUp() throws Exception {
        logExporter = InMemoryLogRecordExporter.create();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider
            .builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
            .build();
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build();
        otelLogger = new OpenTelemetryLogger(openTelemetrySdk);
        otelLogger.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        otelLogger.stop();
    }

    @Test
    void should_record_log_with_body_only(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        otelLogger.record(vertxContext, "my-log-body");

        List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getBody().asString()).isEqualTo("my-log-body");
        assertThat(logs.get(0).getAttributes().isEmpty()).isTrue();
    }

    @Test
    void should_record_log_with_string_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        otelLogger.record(vertxContext, "my-log-body", Map.of("key", "string-value"));

        List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getBody().asString()).isEqualTo("my-log-body");
        assertThat(logs.get(0).getAttributes().get(AttributeKey.stringKey("key"))).isEqualTo("string-value");
    }

    @Test
    void should_record_log_with_integer_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        otelLogger.record(vertxContext, "my-log-body", Map.of("count", 42));

        List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAttributes().get(AttributeKey.longKey("count"))).isEqualTo(42L);
    }

    @Test
    void should_record_log_with_long_attribute(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        otelLogger.record(vertxContext, "my-log-body", Map.of("timestamp", 1_234_567_890L));

        List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAttributes().get(AttributeKey.longKey("timestamp"))).isEqualTo(1_234_567_890L);
    }

    @Test
    void should_record_log_with_unknown_type_as_string(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        otelLogger.record(vertxContext, "my-log-body", Map.of("flag", true));

        List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAttributes().get(AttributeKey.stringKey("flag"))).isEqualTo("true");
    }

    @Test
    void should_record_log_with_multiple_attributes(Vertx vertx) {
        var vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        otelLogger.record(vertxContext, "my-log-body", Map.of("service", "my-service", "duration", 100L));

        List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getBody().asString()).isEqualTo("my-log-body");
        assertThat(logs.get(0).getAttributes().get(AttributeKey.stringKey("service"))).isEqualTo("my-service");
        assertThat(logs.get(0).getAttributes().get(AttributeKey.longKey("duration"))).isEqualTo(100L);
    }
}
