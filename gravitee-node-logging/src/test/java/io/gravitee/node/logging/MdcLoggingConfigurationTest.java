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
package io.gravitee.node.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MdcLoggingConfigurationTest {

    @Test
    void should_have_default_values() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration();

        assertThat(config.getInclude()).isEmpty();
        assertThat(config.getFormat()).isEqualTo("{key}: {value}");
        assertThat(config.getNullValue()).isEmpty();
        assertThat(config.getSeparator()).isEqualTo(" ");
    }

    @Test
    void should_include_all_keys_when_include_list_is_empty() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration();

        assertThat(config.shouldInclude("anyKey")).isTrue();
        assertThat(config.shouldInclude("anotherKey")).isTrue();
    }

    @Test
    void should_only_include_configured_keys() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(List.of("nodeId", "nodeHostname"), null, null, null);

        assertThat(config.shouldInclude("nodeId")).isTrue();
        assertThat(config.shouldInclude("nodeHostname")).isTrue();
        assertThat(config.shouldInclude("nodeApplication")).isFalse();
    }

    @Test
    void should_preserve_include_list_order() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(List.of("c", "a", "b"), null, null, null);

        assertThat(config.getInclude()).containsExactly("c", "a", "b");
    }

    @Test
    void should_handle_null_include_list() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(null, null, null, null);

        assertThat(config.getInclude()).isEmpty();
        assertThat(config.shouldInclude("anyKey")).isTrue();
    }

    @Test
    void should_use_default_format_when_null_or_empty() {
        MdcLoggingConfiguration configNull = new MdcLoggingConfiguration(null, null, null, null);
        MdcLoggingConfiguration configEmpty = new MdcLoggingConfiguration(null, "", null, null);

        assertThat(configNull.getFormat()).isEqualTo("{key}: {value}");
        assertThat(configEmpty.getFormat()).isEqualTo("{key}: {value}");
    }

    @Test
    void should_use_custom_format() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(null, "{key}={value}", null, null);

        assertThat(config.getFormat()).isEqualTo("{key}={value}");
    }

    @Test
    void should_handle_null_null_value() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(null, null, null, null);

        assertThat(config.getNullValue()).isEmpty();
    }

    @Test
    void should_use_custom_null_value() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(null, null, "-", null);

        assertThat(config.getNullValue()).isEqualTo("-");
    }

    @Test
    void should_use_default_separator_when_null() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(null, null, null, null);

        assertThat(config.getSeparator()).isEqualTo(" ");
    }

    @Test
    void should_use_custom_separator() {
        MdcLoggingConfiguration config = new MdcLoggingConfiguration(null, null, null, " | ");

        assertThat(config.getSeparator()).isEqualTo(" | ");
    }
}
