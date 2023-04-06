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
package io.gravitee.node.vertx.server.http;

import static io.gravitee.node.vertx.server.http.VertxHttpServer.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
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
class VertxHttpServerTest {

    public static final String ID = "my-http";

    @Mock
    private Vertx vertx;

    @Mock
    private VertxHttpServerOptions options;

    @Mock
    private HttpServer delegate;

    @Mock
    private HttpServerOptions vertxHttpOptions;

    private VertxHttpServer cut;

    @BeforeEach
    void init() {
        cut = new VertxHttpServer(ID, vertx, options);
    }

    @Test
    void should_return_id() {
        assertThat(cut.id()).isEqualTo(ID);
    }

    @Test
    void should_return_http_type() {
        assertThat(cut.type()).isEqualTo(TYPE);
    }

    @Test
    void should_instantiate_vertx_http_server() {
        when(options.createHttpServerOptions()).thenReturn(vertxHttpOptions);
        when(vertx.createHttpServer(vertxHttpOptions)).thenReturn(delegate);

        final HttpServer httpServer = cut.newInstance();

        assertThat(httpServer).isNotNull();
        assertThat(cut.instances()).containsExactly(httpServer);
    }

    @Test
    void should_instantiate_multiple_vertx_http_servers() {
        when(options.createHttpServerOptions()).thenReturn(vertxHttpOptions);
        when(vertx.createHttpServer(vertxHttpOptions)).thenReturn(delegate);

        final HttpServer httpServer1 = cut.newInstance();
        final HttpServer httpServer2 = cut.newInstance();
        final HttpServer httpServer3 = cut.newInstance();

        assertThat(cut.instances()).containsExactly(httpServer1, httpServer2, httpServer3);
    }
}
