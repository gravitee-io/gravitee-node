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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.node.api.Node;
import io.gravitee.node.logging.fakes.FakeNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class NodeAwareLoggerTest {

    private Logger delegate;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setUp() {
        delegate = (Logger) LoggerFactory.getLogger("test-node-aware-logger");

        listAppender = new ListAppender<>();
        listAppender.start();
        delegate.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        NodeAwareLogger.flushLogEntry();
        if (delegate != null && listAppender != null) {
            delegate.detachAppender(listAppender);
        }
        MDC.clear();
    }

    @Test
    public void should_throw_when_node_is_null() {
        assertThatThrownBy(() -> new NodeAwareLogger((Node) null, delegate))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Node must not be null");
    }

    @Test
    public void should_throw_when_logger_is_null() {
        assertThatThrownBy(() -> new NodeAwareLogger(new FakeNode("node-id", "hostname"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Delegate logger must not be null");
    }

    @Test
    public void should_log_info_with_node_mdc() {
        Node node = new FakeNode("my-node", "my-host");
        NodeAwareLogger logger = new NodeAwareLogger(node, delegate);

        logger.info("Hello {}", "world");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).isEqualTo("Hello world");
        assertThat(event.getMDCPropertyMap()).containsEntry("nodeId", "my-node").containsEntry("nodeHostname", "my-host");
    }

    @Test
    public void should_log_error_with_throwable_and_node_mdc() {
        Node node = new FakeNode("node-42", "host-42");
        NodeAwareLogger logger = new NodeAwareLogger(node, delegate);

        RuntimeException ex = new RuntimeException("boom");
        logger.error("Oops!", ex);

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).isEqualTo("Oops!");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
        assertThat(event.getMDCPropertyMap()).containsEntry("nodeId", "node-42").containsEntry("nodeHostname", "host-42");
    }

    @Test
    public void should_log_info_with_node_mdc_waiting_for_node_initialization() {
        Node node = new FakeNode("my-node", "my-host");
        AtomicReference<Supplier<Node>> nodeSupplierRef = new AtomicReference<>();
        NodeAwareLogger logger = new NodeAwareLogger(nodeSupplierRef, delegate);

        logger.info("Hello {}", "world");
        nodeSupplierRef.set(() -> node);
        logger.info("Hello {}, with node info", "world");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(2);
        ILoggingEvent eventWithoutNodeInfo = events.get(0);
        assertThat(eventWithoutNodeInfo.getLevel()).isEqualTo(Level.INFO);
        assertThat(eventWithoutNodeInfo.getFormattedMessage()).isEqualTo("Hello world");
        assertThat(eventWithoutNodeInfo.getMDCPropertyMap())
            .doesNotContainEntry("nodeId", "my-node")
            .doesNotContainEntry("nodeHostname", "my-host");

        ILoggingEvent eventWithNodeInfo = events.get(1);
        assertThat(eventWithNodeInfo.getLevel()).isEqualTo(Level.INFO);
        assertThat(eventWithNodeInfo.getFormattedMessage()).isEqualTo("Hello world, with node info");
        assertThat(eventWithNodeInfo.getMDCPropertyMap()).containsEntry("nodeId", "my-node").containsEntry("nodeHostname", "my-host");
    }
}
