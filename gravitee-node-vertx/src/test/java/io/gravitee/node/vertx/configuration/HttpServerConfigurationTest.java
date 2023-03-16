/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.vertx.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpServerConfigurationTest {

    @Spy
    private MockEnvironment environment = new MockEnvironment();

    @BeforeEach
    void init() {
        environment.setProperty("listeners.http.host", "0.0.0.0");
        environment.setProperty("listeners.http.port", "8080");
        environment.setProperty("listeners.http.secured", "false");

        environment.setProperty("listeners.https.host", "0.0.0.0");
        environment.setProperty("listeners.https.secured", "true");
    }

    @Test
    void shouldParseListenersHttpSection() {
        HttpServerConfiguration configuration = HttpServerConfiguration
            .builder()
            .withEnvironment(environment)
            .withDefaultPort(8082) // will not be set, env has a port already
            .build();

        assertFalse(configuration.isSecured());
        assertEquals("env port should get priority", 8080, configuration.getPort());
        assertEquals("default host should be set", "0.0.0.0", configuration.getHost());
    }

    @Test
    void shouldParseListenersHttpsSection() {
        HttpServerConfiguration configuration = HttpServerConfiguration
            .builder()
            .withPrefix("listeners.https")
            .withEnvironment(environment)
            .withDefaultPort(443)
            .build();

        assertTrue(configuration.isSecured());
        assertEquals("default port should be set", 443, configuration.getPort());
        assertEquals("default host should be set", "0.0.0.0", configuration.getHost());
    }
}
