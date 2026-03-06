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

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.gravitee.node.logging.MdcLoggingConfiguration;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A custom logback conversion word ({@code %mdcList}) that renders MDC entries
 * in the order defined by the {@link MdcLoggingConfiguration} include list.
 * <p>
 * Unlike {@code %X{key}}, this converter:
 * <ul>
 *     <li>Iterates the include list in declared order, ensuring consistent rendering</li>
 *     <li>Formats each entry using the configured format pattern (e.g., {@code {key}: {value}})</li>
 *     <li>Separates entries using the configured separator (default: single space)</li>
 *     <li>Substitutes the configured nullValue for missing MDC entries</li>
 *     <li>Does not pollute the MDC map — formatting is done at rendering time only</li>
 * </ul>
 * <p>
 * To register this converter in logback.xml:
 * <pre>{@code
 * <conversionRule conversionWord="mdcList"
 *                 converterClass="io.gravitee.node.container.logback.MdcListConverter" />
 * }</pre>
 */
public class MdcListConverter extends ClassicConverter {

    /**
     * The logback conversion word that maps to this converter.
     * Must be registered via {@code <conversionRule conversionWord="mdcList" .../>} in logback.xml,
     * or programmatically on the {@link ch.qos.logback.classic.LoggerContext}.
     */
    public static final String CONVERSION_WORD = "mdcList";

    /**
     * Converts the logging event into a formatted string of MDC entries.
     * <p>
     * Returns an empty string when:
     * <ul>
     *     <li>No {@link MdcLoggingConfiguration} has been initialized yet (e.g., before gravitee.yml is loaded)</li>
     *     <li>The include list is empty — without an explicit list, there are no keys to render in a
     *         deterministic order. Individual MDC values remain accessible via standard {@code %X{key}} patterns.</li>
     * </ul>
     * <p>
     * When the include list is populated, each key is rendered using the configured format pattern,
     * with missing MDC values replaced by the configured nullValue.
     *
     * @param event the logback logging event providing the MDC property map
     * @return the formatted MDC string, or an empty string if no keys are configured
     */
    @Override
    public String convert(ILoggingEvent event) {
        MdcLoggingConfiguration config = NodeLoggerFactory.getMdcConfiguration();
        if (config == null) {
            return "";
        }

        List<String> includeKeys = config.getInclude();
        if (includeKeys.isEmpty()) {
            return "";
        }

        Map<String, String> eventMDCPropertyMap = event.getMDCPropertyMap();
        String format = config.getFormat();
        String nullValue = config.getNullValue();

        StringJoiner joiner = new StringJoiner(config.getSeparator());
        for (String key : includeKeys) {
            String value = eventMDCPropertyMap.get(key);
            String resolved = value != null ? value : nullValue;
            joiner.add(format.replace("{key}", key).replace("{value}", resolved));
        }

        return joiner.toString();
    }
}
