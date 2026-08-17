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
package io.gravitee.node.jetty.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.healthcheck.Result;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class JettyHttpServerProbeTest {

    @Mock
    private Configuration configuration;

    private Vertx vertx;
    private NetServer server;
    private CountDownLatch serverSideClose;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close().toCompletionStage().toCompletableFuture().join();
        }
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    @Test
    void shouldBeHealthyWhenServerIsListening() throws Exception {
        givenConfiguration(startServer());
        JettyHttpServerProbe cut = new JettyHttpServerProbe(configuration, vertx);

        Result result = cut.check().toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    void shouldBeUnhealthyWhenServerIsNotListening() throws Exception {
        int port = startServer();
        server.close().toCompletionStage().toCompletableFuture().join();
        server = null;
        givenConfiguration(port);
        JettyHttpServerProbe cut = new JettyHttpServerProbe(configuration, vertx);

        Result result = cut.check().toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertThat(result.isHealthy()).isFalse();
    }

    @Test
    void shouldCloseSocketAfterCheck() throws Exception {
        givenConfiguration(startServer());
        JettyHttpServerProbe cut = new JettyHttpServerProbe(configuration, vertx);

        assertThat(cut.check().toCompletableFuture().get(10, TimeUnit.SECONDS).isHealthy()).isTrue();

        assertThat(serverSideClose.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void shouldNotRetainANetClientPerCheck() throws Exception {
        givenConfiguration(startServer());
        JettyHttpServerProbe cut = new JettyHttpServerProbe(configuration, vertx);

        assertThat(cut.check().toCompletableFuture().get(10, TimeUnit.SECONDS).isHealthy()).isTrue();
        long retainedAfterFirstCheck = retainedNetClients();

        for (int i = 0; i < 9; i++) {
            assertThat(cut.check().toCompletableFuture().get(10, TimeUnit.SECONDS).isHealthy()).isTrue();
        }

        assertThat(retainedNetClients()).isEqualTo(retainedAfterFirstCheck);
    }

    private long retainedNetClients() throws Exception {
        // reflection: the clients Vert.x retains are not exposed by any public API, which is the leak this test guards
        Object closeFuture = ((VertxInternal) vertx).closeFuture();
        Field childrenField = closeFuture.getClass().getDeclaredField("children");
        childrenField.setAccessible(true);
        Map<?, ?> children = (Map<?, ?>) childrenField.get(closeFuture);
        return children == null ? 0 : children.keySet().stream().filter(NetClient.class::isInstance).count();
    }

    private void givenConfiguration(int port) {
        when(configuration.getProperty("jetty.port", Integer.class, 8093)).thenReturn(port);
        when(configuration.getProperty("jetty.host", "localhost")).thenReturn("localhost");
    }

    private int startServer() {
        serverSideClose = new CountDownLatch(1);
        server =
            vertx
                .createNetServer()
                .connectHandler(socket -> socket.closeHandler(event -> serverSideClose.countDown()))
                .listen(0, "localhost")
                .toCompletionStage()
                .toCompletableFuture()
                .join();
        return server.actualPort();
    }
}
