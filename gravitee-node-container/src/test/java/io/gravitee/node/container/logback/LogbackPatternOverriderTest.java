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
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogbackPatternOverriderTest {

    private Logger rootLogger;
    private ConsoleAppender<ILoggingEvent> consoleAppender;
    private FileAppender<ILoggingEvent> fileAppender;
    private PatternLayoutEncoder consoleEncoder;
    private PatternLayoutEncoder fileEncoder;

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
        consoleAppender.setName("test-console");
        consoleAppender.start();
        rootLogger.addAppender(consoleAppender);

        fileEncoder = new PatternLayoutEncoder();
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern("%d{HH:mm:ss} %msg%n");
        fileEncoder.start();

        fileAppender = new FileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setEncoder(fileEncoder);
        fileAppender.setName("test-file");
        fileAppender.setFile("target/test-logback-override.log");
        fileAppender.start();
        rootLogger.addAppender(fileAppender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(consoleAppender);
        rootLogger.detachAppender(fileAppender);
        consoleAppender.stop();
        fileAppender.stop();
    }

    @Test
    void should_override_console_pattern() {
        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";

        LogbackPatternOverrider.overrideConsolePattern(newPattern);

        assertThat(consoleEncoder.getPattern()).isEqualTo(newPattern);
        assertThat(fileEncoder.getPattern()).isEqualTo("%d{HH:mm:ss} %msg%n");
    }

    @Test
    void should_override_file_pattern() {
        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";

        LogbackPatternOverrider.overrideFilePattern(newPattern);

        assertThat(fileEncoder.getPattern()).isEqualTo(newPattern);
        assertThat(consoleEncoder.getPattern()).isEqualTo("%d{HH:mm:ss} %msg%n");
    }

    @Test
    void should_register_mdcList_converter() {
        LogbackPatternOverrider.registerMdcListConverter();

        assertThat(PatternLayout.DEFAULT_CONVERTER_MAP).containsEntry("mdcList", MdcListConverter.class.getName());
    }
}
