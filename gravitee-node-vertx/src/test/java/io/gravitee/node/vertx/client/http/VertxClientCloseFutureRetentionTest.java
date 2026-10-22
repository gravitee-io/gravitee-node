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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Guards the shared-client workaround of {@link VertxHttpClientFactory#sharedClientName(boolean, String)}: clients must
 * leave the close future of the context that created them once closed, including after they opened real connections.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxClientCloseFutureRetentionTest {

    private static final int CLIENTS = 10;
    private static final int EVENT_LOOPS = 2;

    private Vertx vertx;
    private HttpServer server1;
    private HttpServer server2;
    private Configuration nodeConfiguration;
    private final List<Context> eventLoops = new ArrayList<>();

    @BeforeEach
    void setUp() {
        nodeConfiguration = mock(Configuration.class);
        when(nodeConfiguration.getProperty(anyString(), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        vertx = Vertx.vertx();
        server1 = startServer("server1");
        server2 = startServer("server2");
        // Each verticle runs on its own event loop context, like the instances of the gateway HTTP server verticle.
        for (int i = 0; i < EVENT_LOOPS; i++) {
            AtomicReference<Context> started = new AtomicReference<>();
            vertx
                .rxDeployVerticle(
                    new AbstractVerticle() {
                        @Override
                        public void start() {
                            started.set(context);
                        }
                    }
                )
                .blockingGet();
            eventLoops.add(started.get());
        }
    }

    @AfterEach
    void tearDown() {
        vertx.rxClose().blockingAwait();
    }

    @Test
    void should_not_retain_closed_http_clients_off_a_vertx_context() throws Exception {
        CloseFuture root = rootCloseFuture();
        int before = retained(root);

        for (int i = 0; i < CLIENTS; i++) {
            HttpClient client = httpClient(server1, false, null);
            assertThat(body(client).blockingGet()).isEqualTo("server1");
            client.rxClose().blockingAwait();
        }

        assertThat(awaitRetained(root, before) - before).isZero();
    }

    @Test
    void should_not_retain_closed_http_clients_on_event_loop_contexts() throws Exception {
        assertNoRetentionOnEventLoops(() -> httpClient(server1, false, null), client -> body(client).ignoreElement(), HttpClient::rxClose);
    }

    @Test
    void should_not_retain_closed_websocket_clients_off_a_vertx_context() throws Exception {
        CloseFuture root = rootCloseFuture();
        int before = retained(root);

        for (int i = 0; i < CLIENTS; i++) {
            WebSocketClient client = webSocketClient(server1);
            client.rxConnect("/").blockingGet();
            client.rxClose().blockingAwait();
        }

        assertThat(awaitRetained(root, before) - before).isZero();
    }

    @Test
    void should_not_retain_closed_websocket_clients_on_event_loop_contexts() throws Exception {
        assertNoRetentionOnEventLoops(
            () -> webSocketClient(server1),
            client -> client.rxConnect("/").ignoreElement(),
            WebSocketClient::rxClose
        );
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
            assertThat(body(httpClient).blockingGet()).isEqualTo("server1");
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
            assertThat(body(first).blockingGet()).isEqualTo("server1");
            assertThat(body(second).blockingGet()).isEqualTo("server2");
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
            assertThat(body(second).blockingGet()).isEqualTo("server1");
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
    void should_still_leak_when_the_client_comes_straight_from_vertx() throws Exception {
        // This watches the Vert.x version this module is built against (vertx.version from gravitee-bom), the one the
        // gateway ships with it.
        Context eventLoop = eventLoops.get(0);
        CloseFuture closeFuture = closeFuture(eventLoop);
        int before = retained(closeFuture);

        for (int i = 0; i < CLIENTS; i++) {
            onContext(eventLoop, () -> vertx.getDelegate().createHttpClient(new HttpClientOptions()).close()).await(10, TimeUnit.SECONDS);
        }

        assertThat(retained(closeFuture) - before)
            .as("Vert.x no longer leaks closed clients: remove the shared-client workaround of VertxHttpClientFactory and this test")
            .isEqualTo(CLIENTS);
    }

    /**
     * Creates clients on every event loop, opens a real connection with each and keeps it open, then closes them all from
     * the test thread, as the deployer does: closing has to close the connections before the clients can unregister.
     */
    private <C> void assertNoRetentionOnEventLoops(Supplier<C> create, Function<C, Completable> connect, Function<C, Completable> close)
        throws Exception {
        List<Integer> before = eventLoops
            .stream()
            .map(context -> retained(closeFuture(context)))
            .toList();
        List<C> clients = new CopyOnWriteArrayList<>();

        for (int i = 0; i < CLIENTS; i++) {
            for (Context eventLoop : eventLoops) {
                onContext(eventLoop, () -> {
                    C client = create.get();
                    clients.add(client);
                    return toFuture(connect.apply(client));
                }).await(10, TimeUnit.SECONDS);
            }
        }
        assertThat(clients).hasSize(CLIENTS * EVENT_LOOPS);
        Completable.merge(clients.stream().map(close).toList()).blockingAwait();

        for (int i = 0; i < EVENT_LOOPS; i++) {
            CloseFuture closeFuture = closeFuture(eventLoops.get(i));
            assertThat(closeFuture).isNotSameAs(rootCloseFuture());
            assertThat(awaitRetained(closeFuture, before.get(i)) - before.get(i)).as("clients left on event loop %d", i).isZero();
        }
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

    private static Single<String> body(HttpClient client) {
        return client
            .rxRequest(HttpMethod.GET, "/")
            .flatMap(request -> request.rxSend())
            .flatMap(response -> response.rxBody())
            .map(Object::toString);
    }

    private static Future<Void> onContext(Context context, Supplier<Future<Void>> action) {
        Promise<Void> done = Promise.promise();
        context.runOnContext(v -> action.get().onComplete(done));
        return done.future();
    }

    private static Future<Void> toFuture(Completable completable) {
        Promise<Void> done = Promise.promise();
        completable.subscribe(done::complete, done::fail);
        return done.future();
    }

    private CloseFuture rootCloseFuture() {
        return ((VertxInternal) vertx.getDelegate()).closeFuture();
    }

    private static CloseFuture closeFuture(Context context) {
        return ((ContextInternal) context).closeFuture();
    }

    /** Waits for close() to complete asynchronously and returns the last count observed. */
    private static int awaitRetained(CloseFuture closeFuture, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        int retained = retained(closeFuture);
        while (retained != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
            retained = retained(closeFuture);
        }
        return retained;
    }

    private static int retained(CloseFuture closeFuture) {
        try {
            Field children = CloseFuture.class.getDeclaredField("children");
            children.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) children.get(closeFuture);
            return map == null ? 0 : map.size();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "CloseFuture.children is gone: Vert.x internals changed, this does not mean the leak is fixed. " +
                    "Re-check how closed clients are tracked before touching the shared-client workaround.",
                e
            );
        }
    }
}
