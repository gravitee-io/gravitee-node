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

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogbackPatternOverriderTest {

    private static final String ORIGINAL_PATTERN = "%d{HH:mm:ss} %msg%n";

    private LoggerContext loggerContext;
    private Logger rootLogger;
    private final List<String> attachedAppenderNames = new ArrayList<>();

    @BeforeEach
    void setUp() {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    @AfterEach
    void tearDown() {
        for (String name : attachedAppenderNames) {
            Appender<ILoggingEvent> appender = rootLogger.getAppender(name);
            if (appender != null) {
                rootLogger.detachAppender(appender);
                appender.stop();
            }
        }
    }

    @Test
    void should_override_console_pattern() {
        PatternLayoutEncoder consoleEncoder = createEncoder();
        attachAppender(createConsoleAppender("test-console", consoleEncoder));
        PatternLayoutEncoder fileEncoder = createEncoder();
        attachAppender(createFileAppender("test-file", fileEncoder));

        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";
        LogbackPatternOverrider.overrideConsolePattern(newPattern);

        assertThat(consoleEncoder.getPattern()).isEqualTo(newPattern);
        assertThat(fileEncoder.getPattern()).isEqualTo(ORIGINAL_PATTERN);
    }

    @Test
    void should_override_file_pattern() {
        PatternLayoutEncoder consoleEncoder = createEncoder();
        attachAppender(createConsoleAppender("test-console", consoleEncoder));
        PatternLayoutEncoder fileEncoder = createEncoder();
        attachAppender(createFileAppender("test-file", fileEncoder));

        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";
        LogbackPatternOverrider.overrideFilePattern(newPattern);

        assertThat(fileEncoder.getPattern()).isEqualTo(newPattern);
        assertThat(consoleEncoder.getPattern()).isEqualTo(ORIGINAL_PATTERN);
    }

    @Test
    void should_override_console_pattern_wrapped_in_async_appender() {
        PatternLayoutEncoder consoleEncoder = createEncoder();
        attachAppender(wrapInAsync("async-console", createConsoleAppender("STDOUT", consoleEncoder)));

        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";
        LogbackPatternOverrider.overrideConsolePattern(newPattern);

        assertThat(consoleEncoder.getPattern()).isEqualTo(newPattern);
    }

    @Test
    void should_override_file_pattern_wrapped_in_async_appender() {
        PatternLayoutEncoder fileEncoder = createEncoder();
        attachAppender(wrapInAsync("async-file", createFileAppender("FILE", fileEncoder)));

        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";
        LogbackPatternOverrider.overrideFilePattern(newPattern);

        assertThat(fileEncoder.getPattern()).isEqualTo(newPattern);
    }

    @Test
    void should_override_rolling_file_pattern_wrapped_in_async_appender() {
        PatternLayoutEncoder rollingEncoder = createEncoder();
        attachAppender(wrapInAsync("async-rolling", createRollingFileAppender("ROLLING", rollingEncoder)));

        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";
        LogbackPatternOverrider.overrideFilePattern(newPattern);

        assertThat(rollingEncoder.getPattern()).isEqualTo(newPattern);
    }

    @Test
    void should_not_override_console_when_only_file_wrapped_in_async() {
        PatternLayoutEncoder consoleEncoder = createEncoder();
        attachAppender(createConsoleAppender("test-console", consoleEncoder));
        PatternLayoutEncoder fileEncoder = createEncoder();
        attachAppender(wrapInAsync("async-file", createFileAppender("FILE", fileEncoder)));

        String newPattern = "%d{yyyy-MM-dd} %mdcList %msg%n";
        LogbackPatternOverrider.overrideFilePattern(newPattern);

        assertThat(fileEncoder.getPattern()).isEqualTo(newPattern);
        assertThat(consoleEncoder.getPattern()).isEqualTo(ORIGINAL_PATTERN);
    }

    @Test
    void should_register_mdcList_converter() {
        LogbackPatternOverrider.registerMdcListConverter();

        assertThat(PatternLayout.DEFAULT_CONVERTER_MAP).containsEntry("mdcList", MdcListConverter.class.getName());
    }

    private PatternLayoutEncoder createEncoder() {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(ORIGINAL_PATTERN);
        encoder.start();
        return encoder;
    }

    private ConsoleAppender<ILoggingEvent> createConsoleAppender(String name, PatternLayoutEncoder encoder) {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.setName(name);
        appender.start();
        return appender;
    }

    private FileAppender<ILoggingEvent> createFileAppender(String name, PatternLayoutEncoder encoder) {
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.setName(name);
        appender.setFile("target/test-logback-override.log");
        appender.start();
        return appender;
    }

    private RollingFileAppender<ILoggingEvent> createRollingFileAppender(String name, PatternLayoutEncoder encoder) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.setName(name);
        appender.setFile("target/test-logback-rolling-override.log");
        appender.start();
        return appender;
    }

    private AsyncAppender wrapInAsync(String name, Appender<ILoggingEvent> delegate) {
        AsyncAppender async = new AsyncAppender();
        async.setContext(loggerContext);
        async.setName(name);
        async.addAppender(delegate);
        async.start();
        return async;
    }

    private void attachAppender(Appender<ILoggingEvent> appender) {
        rootLogger.addAppender(appender);
        attachedAppenderNames.add(appender.getName());
    }
}
