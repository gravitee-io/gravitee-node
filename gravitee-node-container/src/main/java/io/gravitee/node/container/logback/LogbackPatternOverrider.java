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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import java.util.Iterator;
import org.slf4j.LoggerFactory;

/**
 * Programmatically overrides logback patterns on console and file appenders.
 * <p>
 * Only affects appenders using {@link PatternLayoutEncoder}. JSON and other
 * encoder types are left untouched.
 * </p>
 */
public final class LogbackPatternOverrider {

    private LogbackPatternOverrider() {}

    /**
     * Registers the {@code %mdcList} conversion word on the current logback context.
     */
    public static void registerMdcListConverter() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.putObject(MdcListConverter.CONVERSION_WORD, MdcListConverter.class.getName());
        // Register via the conversionRule map used by PatternLayout
        PatternLayout.DEFAULT_CONVERTER_MAP.put(MdcListConverter.CONVERSION_WORD, MdcListConverter.class.getName());
    }

    /**
     * Overrides the pattern of all console appenders found on the root logger.
     *
     * @param pattern the new pattern to apply
     */
    public static void overrideConsolePattern(String pattern) {
        overridePatternForAppenderType(ConsoleAppender.class, pattern);
    }

    /**
     * Overrides the pattern of all file appenders found on the root logger.
     *
     * @param pattern the new pattern to apply
     */
    public static void overrideFilePattern(String pattern) {
        overridePatternForAppenderType(FileAppender.class, pattern);
    }

    private static void overridePatternForAppenderType(Class<?> appenderType, String pattern) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        Iterator<Appender<ILoggingEvent>> appenders = rootLogger.iteratorForAppenders();
        while (appenders.hasNext()) {
            Appender<ILoggingEvent> appender = appenders.next();
            if (appenderType.isInstance(appender) && appender instanceof ch.qos.logback.core.OutputStreamAppender<?> outputAppender) {
                if (outputAppender.getEncoder() instanceof PatternLayoutEncoder encoder) {
                    encoder.stop();
                    encoder.setPattern(pattern);
                    encoder.start();
                }
            }
        }
    }
}
