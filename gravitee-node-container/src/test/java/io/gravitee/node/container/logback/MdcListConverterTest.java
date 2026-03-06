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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.gravitee.node.logging.MdcLoggingConfiguration;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MdcListConverterTest {

    private final MdcListConverter converter = new MdcListConverter();

    @AfterEach
    void tearDown() {
        NodeLoggerFactory.resetMdcConfiguration();
    }

    @Test
    void should_return_empty_when_no_configuration() {
        ILoggingEvent event = mock(ILoggingEvent.class);

        assertThat(converter.convert(event)).isEmpty();
    }

    @Test
    void should_return_empty_when_include_list_is_empty() {
        NodeLoggerFactory.initMdcConfiguration(new MdcLoggingConfiguration());
        ILoggingEvent event = mock(ILoggingEvent.class);

        assertThat(converter.convert(event)).isEmpty();
    }

    @Test
    void should_render_mdc_entries_in_declared_order() {
        NodeLoggerFactory.initMdcConfiguration(
            new MdcLoggingConfiguration(List.of("nodeId", "nodeHostname", "nodeApplication"), null, null)
        );
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of("nodeId", "node-1", "nodeHostname", "host-1", "nodeApplication", "gw"));

        String result = converter.convert(event);

        assertThat(result).isEqualTo("nodeId=\"node-1\" nodeHostname=\"host-1\" nodeApplication=\"gw\"");
    }

    @Test
    void should_use_null_value_for_missing_mdc_entries() {
        NodeLoggerFactory.initMdcConfiguration(new MdcLoggingConfiguration(List.of("nodeId", "missing"), null, "-"));
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of("nodeId", "node-1"));

        String result = converter.convert(event);

        assertThat(result).isEqualTo("nodeId=\"node-1\" missing=\"-\"");
    }

    @Test
    void should_use_custom_format() {
        NodeLoggerFactory.initMdcConfiguration(new MdcLoggingConfiguration(List.of("nodeId"), "[%s:%s]", null));
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of("nodeId", "node-1"));

        String result = converter.convert(event);

        assertThat(result).isEqualTo("[nodeId:node-1]");
    }

    @Test
    void should_use_empty_string_as_default_null_value() {
        NodeLoggerFactory.initMdcConfiguration(new MdcLoggingConfiguration(List.of("missing"), null, null));
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of());

        String result = converter.convert(event);

        assertThat(result).isEqualTo("missing=\"\"");
    }
}
