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
package io.gravitee.node.vertx.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.CleanableHttpClient;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Guards the shared-client workaround of {@link VertxHttpClientFactory#sharedClientName(boolean, String)}: clients must
 * leave the close future of the context that created them once closed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxClientCloseFutureRetentionTest {

    private static final int CLIENTS = 20;

    private Vertx vertx;
    private HttpServer server1;
    private HttpServer server2;

    @Mock
    private Configuration nodeConfiguration;

    @BeforeEach
    void setUp() {
        lenient()
            .when(nodeConfiguration.getProperty(anyString(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(2));
        vertx = Vertx.vertx();
        server1 = startServer("server1");
        server2 = startServer("server2");
    }

    @AfterEach
    void tearDown() {
        vertx.rxClose().blockingAwait();
    }

    @Test
    void should_not_retain_closed_http_clients_off_a_vertx_context() {
        assertThat(retainedAfterCreateAndClose(() -> httpClient(server1, false, null).rxClose())).isZero();
    }

    @Test
    void should_not_retain_closed_http_clients_on_a_deployment_context() {
        assertThat(retainedOnDeploymentAfterCreateAndClose(() -> httpClient(server1, false, null).rxClose())).isZero();
    }

    @Test
    void should_not_retain_closed_websocket_clients_off_a_vertx_context() {
        assertThat(retainedAfterCreateAndClose(() -> webSocketClient(server1).rxClose())).isZero();
    }

    @Test
    void should_not_retain_closed_websocket_clients_on_a_deployment_context() {
        assertThat(retainedOnDeploymentAfterCreateAndClose(() -> webSocketClient(server1).rxClose())).isZero();
    }

    @Test
    void should_not_leave_shared_client_entries_once_closed() {
        httpClient(server1, false, "test").rxClose().blockingAwait();
        webSocketClient(server1).rxClose().blockingAwait();

        assertThat(vertx.getDelegate().sharedData().getLocalMap("__vertx.shared.httpClients")).isEmpty();
        assertThat(vertx.getDelegate().sharedData().getLocalMap("__vertx.shared.webSocketClients")).isEmpty();
    }

    @Test
    void should_return_clients_that_still_work() {
        HttpClient httpClient = httpClient(server1, false, null);
        WebSocketClient webSocketClient = webSocketClient(server1);
        try {
            assertThat(body(httpClient)).isEqualTo("server1");
            assertThat(webSocketClient.rxConnect("/").blockingGet()).isNotNull();
        } finally {
            httpClient.rxClose().blockingAwait();
            webSocketClient.rxClose().blockingAwait();
        }
    }

    @Test
    void should_not_share_a_pool_between_clients_that_did_not_ask_for_it() {
        HttpClient first = httpClient(server1, false, "test");
        HttpClient second = httpClient(server2, false, "test");
        try {
            assertThat(((CleanableHttpClient) first.getDelegate()).options().getName()).startsWith("test-");
            assertThat(body(first)).isEqualTo("server1");
            assertThat(body(second)).isEqualTo("server2");
        } finally {
            first.rxClose().blockingAwait();
            second.rxClose().blockingAwait();
        }
    }

    @Test
    void should_keep_sharing_clients_that_asked_for_it() {
        HttpClient first = httpClient(server1, true, "test");
        HttpClient second = httpClient(server2, true, "test");
        try {
            // Vert.x keeps the options of the first client created under a shared name.
            assertThat(body(second)).isEqualTo("server1");
        } finally {
            first.rxClose().blockingAwait();
            second.rxClose().blockingAwait();
        }
    }

    @Test
    void should_close_clients_when_the_deployment_that_created_them_is_undeployed() {
        AtomicReference<HttpClient> client = new AtomicReference<>();
        String deploymentId = vertx
            .rxDeployVerticle(
                new AbstractVerticle() {
                    @Override
                    public void start() {
                        client.set(httpClient(server1, false, null));
                    }
                }
            )
            .blockingGet();

        vertx.rxUndeploy(deploymentId).blockingAwait();

        assertThat(
            client
                .get()
                .rxRequest(HttpMethod.GET, "/")
                .map(r -> true)
                .onErrorReturnItem(false)
                .blockingGet()
        ).isFalse();
    }

    @Test
    void should_still_leak_when_the_client_comes_straight_from_vertx() {
        int before = retained(rootCloseFuture());

        for (int i = 0; i < CLIENTS; i++) {
            vertx.createHttpClient(new HttpClientOptions()).rxClose().blockingAwait();
        }

        // When this starts failing, Vert.x has been fixed (5.2): drop the shared-client workaround and this class.
        assertThat(retained(rootCloseFuture()) - before).isEqualTo(CLIENTS);
    }

    private HttpServer startServer(String body) {
        return vertx
            .createHttpServer()
            .webSocketHandler(ws -> {})
            .requestHandler(request -> request.response().end(body))
            .rxListen(0, "localhost")
            .blockingGet();
    }

    private HttpClient httpClient(HttpServer server, boolean shared, String name) {
        return VertxHttpClientFactory.builder()
            .vertx(vertx)
            .nodeConfiguration(nodeConfiguration)
            .defaultTarget("http://localhost:" + server.actualPort())
            .shared(shared)
            .name(name)
            .build()
            .createHttpClient();
    }

    private WebSocketClient webSocketClient(HttpServer server) {
        return VertxWebSocketClientFactory.builder()
            .vertx(vertx)
            .nodeConfiguration(nodeConfiguration)
            .defaultTarget("http://localhost:" + server.actualPort())
            .build()
            .createWebSocketClient();
    }

    private static String body(HttpClient client) {
        return client
            .rxRequest(HttpMethod.GET, "/")
            .flatMap(request -> request.rxSend())
            .flatMap(response -> response.rxBody())
            .blockingGet()
            .toString();
    }

    private int retainedAfterCreateAndClose(Supplier<Completable> createAndClose) {
        int before = retained(rootCloseFuture());
        for (int i = 0; i < CLIENTS; i++) {
            createAndClose.get().blockingAwait();
        }
        return retained(rootCloseFuture()) - before;
    }

    private int retainedOnDeploymentAfterCreateAndClose(Supplier<Completable> createAndClose) {
        // Clients created inside a verticle register on that deployment's close future, not the root one.
        AtomicReference<CloseFuture> deploymentCloseFuture = new AtomicReference<>();
        AtomicReference<Integer> before = new AtomicReference<>();
        List<Completable> closes = new ArrayList<>();
        vertx
            .rxDeployVerticle(
                new AbstractVerticle() {
                    @Override
                    public void start() {
                        deploymentCloseFuture.set(((ContextInternal) context).closeFuture());
                        before.set(retained(deploymentCloseFuture.get()));
                        for (int i = 0; i < CLIENTS; i++) {
                            closes.add(createAndClose.get());
                        }
                    }
                }
            )
            .blockingGet();
        assertThat(deploymentCloseFuture.get()).isNotSameAs(rootCloseFuture());
        Completable.merge(closes).blockingAwait();
        return retained(deploymentCloseFuture.get()) - before.get();
    }

    private CloseFuture rootCloseFuture() {
        return ((VertxInternal) vertx.getDelegate()).closeFuture();
    }

    private static int retained(CloseFuture closeFuture) {
        try {
            Field children = CloseFuture.class.getDeclaredField("children");
            children.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) children.get(closeFuture);
            return map == null ? 0 : map.size();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
