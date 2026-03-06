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
import ch.qos.logback.core.ConsoleAppender;
import io.gravitee.node.logging.MdcLoggingConfiguration;
import io.gravitee.node.logging.NodeLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingOverrideConfigurationTest {

    private ConsoleAppender<ILoggingEvent> consoleAppender;
    private PatternLayoutEncoder consoleEncoder;
    private Logger rootLogger;

    @BeforeEach
    void setUp() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(loggerContext);
        consoleEncoder.setPattern("%d{HH:mm:ss} %msg%n");
        consoleEncoder.start();

        consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.setName("test-integration-console");
        consoleAppender.start();
        rootLogger.addAppender(consoleAppender);
    }

    @AfterEach
    void tearDown() {
        NodeLoggerFactory.resetMdcConfiguration();
        rootLogger.detachAppender(consoleAppender);
        consoleAppender.stop();
    }

    @Test
    void should_configure_mdc_from_environment_properties() {
        MockEnvironment env = new MockEnvironment()
            .withProperty("logging.mdc.include[0]", "nodeId")
            .withProperty("logging.mdc.include[1]", "nodeHostname")
            .withProperty("logging.mdc.format", "[%s=%s]")
            .withProperty("logging.mdc.nullValue", "N/A");

        LoggingOverrideConfiguration config = new LoggingOverrideConfiguration(env);
        config.configure();

        MdcLoggingConfiguration mdcConfig = NodeLoggerFactory.getMdcConfiguration();
        assertThat(mdcConfig).isNotNull();
        assertThat(mdcConfig.getInclude()).containsExactly("nodeId", "nodeHostname");
        assertThat(mdcConfig.getFormat()).isEqualTo("[%s=%s]");
        assertThat(mdcConfig.getNullValue()).isEqualTo("N/A");
    }

    @Test
    void should_not_override_patterns_when_flag_is_false() {
        MockEnvironment env = new MockEnvironment()
            .withProperty("logging.pattern.overrideLogbackXml", "false")
            .withProperty("logging.pattern.console", "%d %msg%n");

        LoggingOverrideConfiguration config = new LoggingOverrideConfiguration(env);
        config.configure();

        assertThat(consoleEncoder.getPattern()).isEqualTo("%d{HH:mm:ss} %msg%n");
    }

    @Test
    void should_override_console_pattern_when_flag_is_true() {
        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";
        MockEnvironment env = new MockEnvironment()
            .withProperty("logging.pattern.overrideLogbackXml", "true")
            .withProperty("logging.pattern.console", newPattern);

        LoggingOverrideConfiguration config = new LoggingOverrideConfiguration(env);
        config.configure();

        assertThat(consoleEncoder.getPattern()).isEqualTo(newPattern);
    }

    @Test
    void should_use_defaults_when_no_properties_set() {
        MockEnvironment env = new MockEnvironment();

        LoggingOverrideConfiguration config = new LoggingOverrideConfiguration(env);
        config.configure();

        MdcLoggingConfiguration mdcConfig = NodeLoggerFactory.getMdcConfiguration();
        assertThat(mdcConfig).isNotNull();
        assertThat(mdcConfig.getInclude()).isEmpty();
        assertThat(mdcConfig.getFormat()).isEqualTo("%s=\"%s\"");
        assertThat(mdcConfig.getNullValue()).isEmpty();
        assertThat(mdcConfig.isFilterAll()).isFalse();
    }

    @Test
    void should_configure_filterAll_from_environment() {
        MockEnvironment env = new MockEnvironment()
            .withProperty("logging.mdc.include[0]", "nodeId")
            .withProperty("logging.mdc.filterAll", "true");

        LoggingOverrideConfiguration config = new LoggingOverrideConfiguration(env);
        config.configure();

        MdcLoggingConfiguration mdcConfig = NodeLoggerFactory.getMdcConfiguration();
        assertThat(mdcConfig).isNotNull();
        assertThat(mdcConfig.isFilterAll()).isTrue();
    }
}
