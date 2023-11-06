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
package io.gravitee.node.vertx.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.gravitee.node.vertx.server.tcp.VertxTcpServerOptions;
import io.vertx.rxjava3.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxServerFactoryTest {

    @Mock
    Vertx vertx;

    private VertxServerFactory<?, VertxServerOptions> cut;

    @BeforeEach
    void init() {
        cut = new VertxServerFactory<>(vertx);
    }

    @Test
    void should_create_vertx_http_server() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().build();
        final VertxServer<?, VertxServerOptions> vertxServer = cut.create(options);

        assertThat(vertxServer).isNotNull();
        assertThat(vertxServer.options()).isEqualTo(options);
        assertThat(vertxServer.instances()).isEmpty();
    }

    @Test
    void should_create_vertx_net_server() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().build();
        final VertxServer<?, VertxServerOptions> vertxServer = cut.create(options);

        assertThat(vertxServer).isNotNull();
        assertThat(vertxServer.options()).isEqualTo(options);
        assertThat(vertxServer.instances()).isEmpty();
    }

    @Test
    void should_throw_illegal_argument_exception_when_create_unsupported_vertx_server() {
        final VertxServerOptions options = mock(VertxServerOptions.class);
        final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> cut.create(options));

        assertThat(illegalArgumentException.getMessage())
            .isEqualTo("Server type is not a supported vertx server (option class=[VertxServerOptions])");
    }
}
