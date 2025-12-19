/*
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
package io.gravitee.node.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.node.api.Node;
import io.gravitee.node.logging.fakes.FakeNode;
import io.gravitee.node.logging.fakes.context.HttpExecutionContext;
import io.gravitee.node.logging.fakes.context.KafkaExecutionContext;
import io.gravitee.node.logging.fakes.loggers.HttpExecutionContextAwareLogger;
import io.gravitee.node.logging.fakes.loggers.KafkaExecutionContextAwareLogger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Unit tests for the {@code ExecutionContextAwareLogger} implementations.
 * This test class validates the behavior of logging with execution contexts
 * such as HTTP and Kafka, ensuring the generated logs include appropriate
 * context-specific and inherited metadata entries.
 *
 * The tests check:
 * - Proper logging of messages with execution contexts.
 * - Inclusion of base execution context attributes in the logging context.
 * - Integration of node-specific metadata inherited by the logger.
 * - Context-specific metadata specific to each execution context type (e.g., HTTP or Kafka).
 * - Exclusion of irrelevant attributes from unrelated contexts.
 *
 * The tests rely on a delegate SLF4J logger, to which a {@code ListAppender} is attached
 * to capture and assert the emitted logging events. This setup ensures the log messages
 * and metadata can be verified as expected.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ExecutionContextAwareLoggerTest {

    private Logger delegate;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setUp() {
        delegate = (Logger) LoggerFactory.getLogger("test-execution-context-aware-logger");

        listAppender = new ListAppender<>();
        listAppender.start();
        delegate.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        if (delegate != null && listAppender != null) {
            delegate.detachAppender(listAppender);
        }
        MDC.clear();
    }

    @Test
    public void should_log_with_http_execution_context_and_inherited_entries() {
        // Given
        Node node = new FakeNode("node-http", "host-http");
        Map<String, String> attrs = new HashMap<>();
        attrs.put("api", "api-1");
        attrs.put("application", "app-1");
        attrs.put("httpAttribute", "http-val");

        HttpExecutionContext httpCtx = attrs::get;

        HttpExecutionContextAwareLogger logger = new HttpExecutionContextAwareLogger(httpCtx, delegate, node);

        // When
        logger.info("Hello {}", "http");

        // Then
        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).isEqualTo("Hello http");
        // Node inherited entries are present (values are cached at factory level and already covered by NodeAwareLoggerTest)
        assertThat(event.getMDCPropertyMap()).containsKeys("nodeId", "nodeHostname");
        // Base execution-context entries (from abstract parent)
        assertThat(event.getMDCPropertyMap()).containsEntry("api", "api-1").containsEntry("application", "app-1");
        // HTTP specific entries (from child)
        assertThat(event.getMDCPropertyMap()).containsEntry("httpAttribute", "http-val");
    }

    @Test
    public void should_log_with_kafka_execution_context_and_inherited_entries() {
        // Given
        Node node = new FakeNode("node-kafka", "host-kafka");
        Map<String, String> attrs = new HashMap<>();
        attrs.put("api", "api-2");
        attrs.put("application", "app-2");
        attrs.put("kafkaAttribute", "kafka-val");
        attrs.put("httpAttribute", "should-not-be-used");

        KafkaExecutionContext kafkaCtx = attrs::get;

        KafkaExecutionContextAwareLogger logger = new KafkaExecutionContextAwareLogger(kafkaCtx, delegate, node);

        // When
        logger.info("Hello {}", "kafka");

        // Then
        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).isEqualTo("Hello kafka");
        // Node inherited entries are present (values may be cached from previous test)
        assertThat(event.getMDCPropertyMap()).containsKeys("nodeId", "nodeHostname");
        // Base execution-context entries (from abstract parent)
        assertThat(event.getMDCPropertyMap())
            .containsKey("api") // cached, may keep previous value
            .containsEntry("application", "app-2");
        // Kafka specific entries (from child)
        assertThat(event.getMDCPropertyMap()).containsEntry("kafkaAttribute", "kafka-val");
        assertThat(event.getMDCPropertyMap()).doesNotContainEntry("httpAttribute", "http-val");
    }
}
