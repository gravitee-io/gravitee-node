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
package io.gravitee.node.container.logback;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import io.gravitee.node.api.Node;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.env.MockEnvironment;

/**
 * End-to-end test that validates the full logging override chain:
 * gravitee.yml properties -> MdcLoggingConfiguration -> MDC filtering -> %mdcList rendering -> final log output.
 * <p>
 * A single Node is used for the whole test class, reflecting the real-world constraint
 * that one Node exists per JVM and its log entries (nodeId, nodeHostname, nodeApplication) are cached.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingEndToEndTest {

    private static final String NODE_ID = "gw-1";
    private static final String NODE_HOSTNAME = "host-alpha";
    private static final String NODE_APPLICATION = "gateway";

    private LoggerContext loggerContext;
    private OutputStreamAppender<ILoggingEvent> appender;
    private ByteArrayOutputStream outputStream;
    private Logger logbackLogger;

    @BeforeAll
    static void initNode() {
        NodeLoggerFactory.init(new FakeNode(NODE_ID, NODE_HOSTNAME, NODE_APPLICATION));
    }

    @BeforeEach
    void setUp() {
        NodeLoggerFactory.resetMdcConfiguration();
        MDC.clear();
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        logbackLogger = loggerContext.getLogger("e2e-test-" + System.nanoTime());

        outputStream = new ByteArrayOutputStream();
        appender = new OutputStreamAppender<>();
        appender.setContext(loggerContext);
        appender.setName("test-e2e");
    }

    @AfterEach
    void tearDown() {
        NodeLoggerFactory.resetMdcConfiguration();
        if (logbackLogger != null && appender != null) {
            logbackLogger.detachAppender(appender);
        }
        appender.stop();
    }

    @Test
    void should_render_mdcList_with_filtered_keys() {
        configureMdc(
            new MockEnvironment()
                .withProperty("logging.mdc.include[0]", "nodeId")
                .withProperty("logging.mdc.include[1]", "nodeHostname")
                .withProperty("logging.mdc.format", "[%s: %s]")
                .withProperty("logging.mdc.nullValue", "-")
        );
        startAppenderWithPattern("%mdcList %msg%n");

        org.slf4j.Logger logger = NodeLoggerFactory.getLogger(logbackLogger.getName());
        logger.info("request processed");

        String output = getOutput();
        // nodeApplication is filtered out, nodeId and nodeHostname are formatted with custom format
        assertThat(output).isEqualTo("[nodeId: gw-1] [nodeHostname: host-alpha] request processed");
    }

    @Test
    void should_filter_individual_mdc_keys_via_percent_X() {
        configureMdc(new MockEnvironment().withProperty("logging.mdc.include[0]", "nodeId"));
        startAppenderWithPattern("[%X{nodeId}] [%X{nodeHostname}] %msg%n");

        org.slf4j.Logger logger = NodeLoggerFactory.getLogger(logbackLogger.getName());
        logger.info("test message");

        String output = getOutput();
        // nodeHostname is empty because it was filtered from MDC at population time
        assertThat(output).isEqualTo("[gw-1] [] test message");
    }

    @Test
    void should_include_all_mdc_keys_when_no_include_list() {
        configureMdc(new MockEnvironment());
        startAppenderWithPattern("[%X{nodeId}] [%X{nodeHostname}] [%X{nodeApplication}] %msg%n");

        org.slf4j.Logger logger = NodeLoggerFactory.getLogger(logbackLogger.getName());
        logger.info("all keys");

        String output = getOutput();
        assertThat(output).isEqualTo("[gw-1] [host-alpha] [gateway] all keys");
    }

    @Test
    void should_return_empty_mdcList_when_no_include_list() {
        configureMdc(new MockEnvironment());
        startAppenderWithPattern("[%mdcList] %msg%n");

        org.slf4j.Logger logger = NodeLoggerFactory.getLogger(logbackLogger.getName());
        logger.info("no list");

        String output = getOutput();
        // %mdcList returns empty when include list is empty
        assertThat(output).isEqualTo("[] no list");
    }

    @Test
    void should_use_null_value_for_missing_mdc_key_in_mdcList() {
        configureMdc(
            new MockEnvironment()
                .withProperty("logging.mdc.include[0]", "nodeId")
                .withProperty("logging.mdc.include[1]", "nonExistentKey")
                .withProperty("logging.mdc.nullValue", "N/A")
        );
        startAppenderWithPattern("%mdcList %msg%n");

        org.slf4j.Logger logger = NodeLoggerFactory.getLogger(logbackLogger.getName());
        logger.info("with missing");

        String output = getOutput();
        assertThat(output).isEqualTo("nodeId=\"gw-1\" nonExistentKey=\"N/A\" with missing");
    }

    @Test
    void should_filter_external_mdc_keys_when_filterAll_is_enabled() {
        configureMdc(new MockEnvironment().withProperty("logging.mdc.include[0]", "nodeId").withProperty("logging.mdc.filterAll", "true"));
        startAppenderWithPattern("[%X{nodeId}] [%X{externalKey}] %msg%n");

        // Simulate external code (e.g. servlet filter) adding MDC keys
        MDC.put("externalKey", "externalValue");

        org.slf4j.Logger logger = NodeLoggerFactory.getLogger(logbackLogger.getName());
        logger.info("filterAll test");

        String output = getOutput();
        // externalKey should be filtered out because filterAll is enabled
        assertThat(output).isEqualTo("[gw-1] [] filterAll test");
    }

    @Test
    void should_keep_external_mdc_keys_when_filterAll_is_disabled() {
        configureMdc(new MockEnvironment().withProperty("logging.mdc.include[0]", "nodeId").withProperty("logging.mdc.filterAll", "false"));
        startAppenderWithPattern("[%X{nodeId}] [%X{externalKey}] %msg%n");

        MDC.put("externalKey", "externalValue");

        org.slf4j.Logger logger = NodeLoggerFactory.getLogger(logbackLogger.getName());
        logger.info("no filterAll test");

        String output = getOutput();
        // externalKey should remain because filterAll is disabled
        assertThat(output).isEqualTo("[gw-1] [externalValue] no filterAll test");
    }

    private void configureMdc(MockEnvironment env) {
        LoggingOverrideConfiguration config = new LoggingOverrideConfiguration(env);
        config.configure();
    }

    private void startAppenderWithPattern(String pattern) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(pattern);
        encoder.start();

        appender.setEncoder(encoder);
        appender.setOutputStream(outputStream);
        appender.start();
        logbackLogger.addAppender(appender);
    }

    private String getOutput() {
        return outputStream.toString(StandardCharsets.UTF_8).trim();
    }

    /**
     * Minimal Node implementation for testing purposes.
     */
    private record FakeNode(String id, String hostname, String application) implements Node {
        @Override
        public String name() {
            return application;
        }

        @Override
        public io.gravitee.common.component.Lifecycle.State lifecycleState() {
            return io.gravitee.common.component.Lifecycle.State.STARTED;
        }

        @Override
        public Node start() {
            return this;
        }

        @Override
        public Node preStop() {
            return this;
        }

        @Override
        public Node stop() {
            return this;
        }
    }
}
