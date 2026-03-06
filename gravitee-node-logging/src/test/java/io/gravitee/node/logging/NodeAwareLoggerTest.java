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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.internal.verification.VerificationModeFactory.times;

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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
        NodeAwareLogger logger = new NodeAwareLogger(nodeSupplierRef, new AtomicReference<>(), delegate);

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

    @Test
    public void should_filter_mdc_keys_based_on_include_list() {
        AtomicReference<MdcLoggingConfiguration> mdcConfigRef = new AtomicReference<>(
            new MdcLoggingConfiguration(List.of("nodeId"), null, null)
        );
        Node node = new FakeNode("my-node", "my-host");
        NodeAwareLogger logger = new NodeAwareLogger(node, mdcConfigRef, delegate);

        logger.info("Filtered MDC");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getMDCPropertyMap())
            .containsEntry("nodeId", "my-node")
            .doesNotContainKey("nodeHostname")
            .doesNotContainKey("nodeApplication");
    }

    @Test
    public void should_include_all_mdc_keys_when_include_list_is_empty() {
        AtomicReference<MdcLoggingConfiguration> mdcConfigRef = new AtomicReference<>(new MdcLoggingConfiguration(List.of(), null, null));
        Node node = new FakeNode("my-node", "my-host");
        NodeAwareLogger logger = new NodeAwareLogger(node, mdcConfigRef, delegate);

        logger.info("All MDC keys");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getMDCPropertyMap())
            .containsEntry("nodeId", "my-node")
            .containsEntry("nodeHostname", "my-host")
            .containsKey("nodeApplication");
    }

    @Test
    public void should_include_all_mdc_keys_when_no_mdc_configuration() {
        Node node = new FakeNode("my-node", "my-host");
        NodeAwareLogger logger = new NodeAwareLogger(node, delegate);

        logger.info("No MDC config");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getMDCPropertyMap())
            .containsEntry("nodeId", "my-node")
            .containsEntry("nodeHostname", "my-host")
            .containsKey("nodeApplication");
    }

    @Test
    public void should_enrich_mdc_only_once_when_delegate_is_nodeAwareLogger() {
        try (MockedStatic<MDC> mdcMock = Mockito.mockStatic(MDC.class)) {
            Node node = new FakeNode("my-node", "my-host");
            NodeAwareLogger logger = new NodeAwareLogger(node, new NodeAwareLogger(node, delegate));

            logger.info("Hello {}", "world");

            // When the delegate is a NodeAwareLogger, we want to populate the MDC only once
            mdcMock.verify(() -> MDC.put(eq("nodeId"), anyString()), times(1));
            mdcMock.verify(() -> MDC.put(eq("nodeHostname"), anyString()), times(1));
            mdcMock.verify(() -> MDC.put(eq("nodeApplication"), anyString()), times(1));
        }
    }

    @Test
    public void should_filter_external_mdc_keys_when_filterAll_is_enabled() {
        AtomicReference<MdcLoggingConfiguration> mdcConfigRef = new AtomicReference<>(
            new MdcLoggingConfiguration(List.of("nodeId"), null, null, true)
        );
        Node node = new FakeNode("my-node", "my-host");
        NodeAwareLogger logger = new NodeAwareLogger(node, mdcConfigRef, delegate);

        // Simulate an external component (e.g. servlet filter) adding MDC keys
        MDC.put("externalKey", "externalValue");
        MDC.put("anotherExternal", "anotherValue");

        logger.info("FilterAll test");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getMDCPropertyMap())
            .containsEntry("nodeId", "my-node")
            .doesNotContainKey("nodeHostname")
            .doesNotContainKey("externalKey")
            .doesNotContainKey("anotherExternal");
    }

    @Test
    public void should_keep_external_mdc_keys_when_filterAll_is_disabled() {
        AtomicReference<MdcLoggingConfiguration> mdcConfigRef = new AtomicReference<>(
            new MdcLoggingConfiguration(List.of("nodeId"), null, null, false)
        );
        Node node = new FakeNode("my-node", "my-host");
        NodeAwareLogger logger = new NodeAwareLogger(node, mdcConfigRef, delegate);

        // Simulate an external component adding MDC keys
        MDC.put("externalKey", "externalValue");

        logger.info("FilterAll disabled test");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getMDCPropertyMap())
            .containsEntry("nodeId", "my-node")
            .containsEntry("externalKey", "externalValue")
            .doesNotContainKey("nodeHostname");
    }

    @Test
    public void should_not_filter_external_mdc_keys_when_include_list_is_empty() {
        AtomicReference<MdcLoggingConfiguration> mdcConfigRef = new AtomicReference<>(
            new MdcLoggingConfiguration(List.of(), null, null, true)
        );
        Node node = new FakeNode("my-node", "my-host");
        NodeAwareLogger logger = new NodeAwareLogger(node, mdcConfigRef, delegate);

        MDC.put("externalKey", "externalValue");

        logger.info("Empty include with filterAll");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        // When include list is empty, all keys are accepted regardless of filterAll
        assertThat(event.getMDCPropertyMap())
            .containsEntry("nodeId", "my-node")
            .containsEntry("nodeHostname", "my-host")
            .containsEntry("externalKey", "externalValue");
    }

    @Test
    public void should_inherit_mdc_config_from_delegate_nodeAwareLogger() {
        AtomicReference<MdcLoggingConfiguration> mdcConfigRef = new AtomicReference<>(
            new MdcLoggingConfiguration(List.of("nodeId"), null, null)
        );
        Node node = new FakeNode("my-node", "my-host");
        // Inner logger has the config, outer logger uses constructor without explicit mdcConfigRef
        NodeAwareLogger innerLogger = new NodeAwareLogger(node, mdcConfigRef, delegate);
        NodeAwareLogger outerLogger = new NodeAwareLogger(node, innerLogger);

        outerLogger.info("Inherited config");

        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.get(0);
        assertThat(event.getMDCPropertyMap())
            .containsEntry("nodeId", "my-node")
            .doesNotContainKey("nodeHostname")
            .doesNotContainKey("nodeApplication");
    }
}
