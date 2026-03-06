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

import io.gravitee.node.logging.MdcLoggingConfiguration;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring configuration that reads logging override properties from gravitee.yml
 * (via Spring Environment) and applies them to the logging system.
 * <p>
 * This configuration:
 * <ul>
 *     <li>Builds and registers a {@link MdcLoggingConfiguration} into {@link NodeLoggerFactory}</li>
 *     <li>Registers the {@code %mdcList} conversion word programmatically</li>
 *     <li>Overrides logback console/file patterns when {@code logging.pattern.overrideLogbackXml} is {@code true}</li>
 * </ul>
 * <p>
 * Must be registered in the bootstrap classes list to ensure it runs after
 * {@code PropertiesConfiguration} loads gravitee.yml but before the main application context.
 * </p>
 * <p>
 * Properties read from gravitee.yml (also overridable via environment variables):
 * <ul>
 *     <li>{@code logging.mdc.include[0..n]} — ordered list of MDC keys to include</li>
 *     <li>{@code logging.mdc.format} — format pattern for {@code %mdcList} entries (default: {@code %s="%s"})</li>
 *     <li>{@code logging.mdc.nullValue} — value for missing MDC entries (default: empty string)</li>
 *     <li>{@code logging.pattern.overrideLogbackXml} — enable pattern override (default: false)</li>
 *     <li>{@code logging.pattern.console} — console appender pattern</li>
 *     <li>{@code logging.pattern.file} — file appender pattern</li>
 * </ul>
 */
@Configuration
@CustomLog
public class LoggingOverrideConfiguration implements InitializingBean {

    private final Environment environment;

    public LoggingOverrideConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        configure();
    }

    void configure() {
        configureMdc();
        LogbackPatternOverrider.registerMdcListConverter();
        configurePatternOverrides();
    }

    private void configureMdc() {
        List<String> include = readListProperty("logging.mdc.include");
        String format = environment.getProperty("logging.mdc.format");
        String nullValue = environment.getProperty("logging.mdc.nullValue");

        MdcLoggingConfiguration mdcConfig = new MdcLoggingConfiguration(include, format, nullValue);
        NodeLoggerFactory.initMdcConfiguration(mdcConfig);

        if (!include.isEmpty()) {
            log.info("MDC logging configured with include keys: {}", include);
        }
    }

    private void configurePatternOverrides() {
        boolean overrideLogbackXml = environment.getProperty("logging.pattern.overrideLogbackXml", Boolean.class, false);
        if (!overrideLogbackXml) {
            return;
        }

        String consolePattern = environment.getProperty("logging.pattern.console");
        if (consolePattern != null) {
            log.debug("Overriding console appender pattern");
            LogbackPatternOverrider.overrideConsolePattern(consolePattern);
        }

        String filePattern = environment.getProperty("logging.pattern.file");
        if (filePattern != null) {
            log.debug("Overriding file appender pattern");
            LogbackPatternOverrider.overrideFilePattern(filePattern);
        }
    }

    private List<String> readListProperty(String prefix) {
        List<String> result = new ArrayList<>();
        int index = 0;
        while (true) {
            String value = environment.getProperty(prefix + "[" + index + "]");
            if (value == null) {
                break;
            }
            result.add(value);
            index++;
        }
        return result;
    }
}
