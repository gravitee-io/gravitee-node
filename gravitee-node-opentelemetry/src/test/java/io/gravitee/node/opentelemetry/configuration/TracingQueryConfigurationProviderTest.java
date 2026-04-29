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
package io.gravitee.node.opentelemetry.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.vertx.client.http.VertxHttpProxyType;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TracingQueryConfigurationProviderTest {

    private static final String PREFIX = "services.opentelemetry.query.tempo";

    private final MockEnvironment environment = new MockEnvironment();

    @Test
    void should_return_defaults_when_nothing_is_configured() {
        TracingQueryConfiguration cfg = TracingQueryConfigurationProvider.from(environment, PREFIX);

        assertThat(cfg.getUrl()).isEqualTo("http://localhost:3200");
        assertThat(cfg.getHeaders()).isEmpty();
        assertThat(cfg.getSslOptions().isTrustAll()).isFalse();
        assertThat(cfg.getSslOptions().isHostnameVerifier()).isTrue();
        assertThat(cfg.getProxyOptions()).isNull();
        assertThat(cfg.getHttpOptions().getConnectTimeout()).isEqualTo(5000);
        assertThat(cfg.getHttpOptions().getIdleTimeout()).isEqualTo(60000);
    }

    @Test
    void should_read_url_under_the_provided_prefix() {
        environment.setProperty(PREFIX + ".url", "http://tempo:3200");

        assertThat(TracingQueryConfigurationProvider.from(environment, PREFIX).getUrl()).isEqualTo("http://tempo:3200");
    }

    @Test
    void should_resolve_headers_from_bracket_list() {
        environment.setProperty(PREFIX + ".headers[0].name", "X-Scope-OrgID");
        environment.setProperty(PREFIX + ".headers[0].value", "tenant-1");
        environment.setProperty(PREFIX + ".headers[1].name", "Authorization");
        environment.setProperty(PREFIX + ".headers[1].value", "Bearer secret");

        assertThat(TracingQueryConfigurationProvider.from(environment, PREFIX).getHeaders())
            .containsExactlyInAnyOrderEntriesOf(Map.of("X-Scope-OrgID", "tenant-1", "Authorization", "Bearer secret"));
    }

    @Test
    void should_default_header_value_to_empty_string_when_unset() {
        environment.setProperty(PREFIX + ".headers[0].name", "X-Empty");

        assertThat(TracingQueryConfigurationProvider.from(environment, PREFIX).getHeaders()).containsEntry("X-Empty", "");
    }

    @Test
    void should_apply_ssl_overrides() {
        environment.setProperty(PREFIX + ".ssl.trustAll", "true");
        environment.setProperty(PREFIX + ".ssl.verifyHost", "false");

        var ssl = TracingQueryConfigurationProvider.from(environment, PREFIX).getSslOptions();
        assertThat(ssl.isTrustAll()).isTrue();
        assertThat(ssl.isHostnameVerifier()).isFalse();
    }

    @Test
    void should_build_proxy_options_when_enabled() {
        environment.setProperty(PREFIX + ".proxy.enabled", "true");
        environment.setProperty(PREFIX + ".proxy.host", "proxy.internal");
        environment.setProperty(PREFIX + ".proxy.port", "8080");
        environment.setProperty(PREFIX + ".proxy.username", "user");
        environment.setProperty(PREFIX + ".proxy.password", "pass");
        environment.setProperty(PREFIX + ".proxy.type", "SOCKS5");

        var proxy = TracingQueryConfigurationProvider.from(environment, PREFIX).getProxyOptions();
        assertThat(proxy).isNotNull();
        assertThat(proxy.isEnabled()).isTrue();
        assertThat(proxy.getHost()).isEqualTo("proxy.internal");
        assertThat(proxy.getPort()).isEqualTo(8080);
        assertThat(proxy.getUsername()).isEqualTo("user");
        assertThat(proxy.getPassword()).isEqualTo("pass");
        assertThat(proxy.getType()).isEqualTo(VertxHttpProxyType.SOCKS5);
    }

    @Test
    void should_omit_proxy_options_when_disabled() {
        environment.setProperty(PREFIX + ".proxy.host", "proxy.internal");

        assertThat(TracingQueryConfigurationProvider.from(environment, PREFIX).getProxyOptions()).isNull();
    }

    @Test
    void should_apply_http_timeouts() {
        environment.setProperty(PREFIX + ".http.connectTimeout", "1234");
        environment.setProperty(PREFIX + ".http.idleTimeout", "5678");

        var options = TracingQueryConfigurationProvider.from(environment, PREFIX).getHttpOptions();
        assertThat(options.getConnectTimeout()).isEqualTo(1234);
        assertThat(options.getIdleTimeout()).isEqualTo(5678);
    }

    @Test
    void should_isolate_settings_by_prefix() {
        environment.setProperty(PREFIX + ".url", "http://tempo-prod:3200");
        environment.setProperty("apim.tracing.tempo.url", "http://tempo-staging:3200");

        assertThat(TracingQueryConfigurationProvider.from(environment, PREFIX).getUrl()).isEqualTo("http://tempo-prod:3200");
        assertThat(TracingQueryConfigurationProvider.from(environment, "apim.tracing.tempo").getUrl())
            .isEqualTo("http://tempo-staging:3200");
    }
}
