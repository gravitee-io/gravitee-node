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
package io.gravitee.node.vertx.server.tcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.gravitee.node.api.certificate.CRLLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.DefaultCRLLoaderFactoryRegistry;
import io.gravitee.node.certificates.DefaultKeyStoreLoaderFactoryRegistry;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.certificates.crl.FileCRLLoaderFactory;
import io.gravitee.node.certificates.file.FileKeyStoreLoaderFactory;
import io.gravitee.node.certificates.file.FileTrustStoreLoaderFactory;
import io.gravitee.node.vertx.server.VertxServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetServer;
import io.vertx.rxjava3.core.net.NetSocket;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

/**
 * Opens real sockets against a real Vert.x TCP server. The application handler greets every admitted connection
 * with {@code OK\n}, so a client that is refused is one that reads end-of-stream without receiving a byte.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxTcpServerConnectionLimitTest {

    private static final Predicate<SocketAddress> NOBODY_EXEMPT = address -> false;

    private final Vertx vertx = Vertx.vertx();
    private final List<Socket> sockets = new ArrayList<>();
    private final List<NetServer> netServers = new ArrayList<>();
    private final AtomicInteger admitted = new AtomicInteger();
    private final AtomicInteger maxSeenAtAccept = new AtomicInteger();

    @TempDir
    Path tmp;

    @AfterEach
    void tearDown() throws Exception {
        for (Socket s : sockets) {
            try {
                s.close();
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }
        for (NetServer n : netServers) {
            n.close().blockingAwait(5, TimeUnit.SECONDS);
        }
        vertx.close().blockingAwait(5, TimeUnit.SECONDS);
    }

    // ---- AC1, AC6: refused at accept, FIN, no byte (plain server)

    @Test
    void should_close_with_fin_and_no_byte_a_connection_past_the_limit_on_a_plain_server() throws Exception {
        final VertxTcpServer server = plainServer(2, NOBODY_EXEMPT, 0);
        final int port = listen(server);

        final Socket first = connect(port);
        final Socket second = connect(port);
        assertThat(readGreeting(first)).isEqualTo("OK");
        assertThat(readGreeting(second)).isEqualTo("OK");

        final Socket third = connect(port);
        assertThat(readUntilEnd(third)).as("bytes received by a refused client").isEmpty();
        assertThat(server.currentConnections()).isEqualTo(2);
    }

    // ---- AC2: decrement exactly once, whatever closes the connection

    @Test
    void should_release_the_slot_when_the_client_closes() throws Exception {
        final VertxTcpServer server = plainServer(1, NOBODY_EXEMPT, 0);
        final int port = listen(server);

        final Socket client = connect(port);
        assertThat(readGreeting(client)).isEqualTo("OK");
        assertThat(server.currentConnections()).isEqualTo(1);

        client.close();
        waitUntil(() -> server.currentConnections() == 0);
    }

    @Test
    void should_release_the_slot_when_the_client_aborts() throws Exception {
        final VertxTcpServer server = plainServer(1, NOBODY_EXEMPT, 0);
        final int port = listen(server);

        final Socket client = connect(port);
        assertThat(readGreeting(client)).isEqualTo("OK");
        client.setSoLinger(true, 0); // RST instead of FIN
        client.close();

        waitUntil(() -> server.currentConnections() == 0);
    }

    @Test
    void should_release_the_slot_once_when_the_server_closes_and_the_application_sets_its_own_close_handler() throws Exception {
        final VertxTcpServer server = plainServer(1, NOBODY_EXEMPT, 0, socket -> {
            socket.closeHandler(v -> {});
            socket.write("OK\n").subscribe(socket::close);
        });
        final int port = listen(server);

        for (int i = 0; i < 5; i++) {
            final Socket client = connect(port);
            assertThat(readGreeting(client)).isEqualTo("OK");
            client.close();
            waitUntil(() -> server.currentConnections() == 0);
        }
        // a second decrement for the same connection leaves the count at -1, which the wait above never accepts
        assertThat(server.currentConnections()).isZero();
    }

    @Test
    void should_release_the_slot_on_idle_timeout() throws Exception {
        final VertxTcpServer server = plainServer(1, NOBODY_EXEMPT, 1);
        final int port = listen(server);

        final Socket client = connect(port);
        assertThat(readGreeting(client)).isEqualTo("OK");

        client.setSoTimeout(5000);
        assertThat(client.getInputStream().read()).as("server closed the idle connection").isEqualTo(-1);
        waitUntil(() -> server.currentConnections() == 0);
    }

    // ---- AC3: concurrency

    @Test
    void should_never_exceed_the_limit_and_return_to_zero_under_concurrent_connect_and_abort() throws Exception {
        final int limit = 8;
        final VertxTcpServer server = plainServer(limit, NOBODY_EXEMPT, 0);
        final int port = listen(server);

        final int threads = 16;
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final Random random = new Random(t);
            futures.add(
                pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < 150; i++) {
                        try (Socket s = new Socket()) {
                            s.connect(new InetSocketAddress("127.0.0.1", port), 3000);
                            s.setSoTimeout(2000);
                            switch (random.nextInt(4)) {
                                case 0 -> s.setSoLinger(true, 0); // abort at once
                                case 1 -> s.getOutputStream().write(new byte[] { 1, 2, 3 }); // abort after a write
                                case 2 -> s.getInputStream().read(); // abort after the greeting or the refusal
                                default -> s.close(); // plain close (closing again on exit is a no-op)
                            }
                        } catch (IOException ignored) {
                            // the server may reset the connection; that is part of the scenario
                        }
                    }
                    return null;
                })
            );
        }
        start.countDown();
        for (var f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertThat(admitted.get()).isPositive();
        assertThat(maxSeenAtAccept.get()).as("highest count seen by an admitted connection").isLessThanOrEqualTo(limit);
        waitUntil(() -> server.currentConnections() == 0);
    }

    // ---- AC4: loopback

    @Test
    void should_neither_count_nor_refuse_connections_from_127_0_0_1() throws Exception {
        final VertxTcpServer server = plainServer(1, ConnectionLimiter::isLoopback, 0);
        final int port = listen(server);

        for (int i = 0; i < 5; i++) {
            assertThat(readGreeting(connect(port))).isEqualTo("OK");
        }
        assertThat(server.currentConnections()).isZero();
    }

    @Test
    void should_count_a_connection_from_127_0_0_6() throws Exception {
        // Linux only: macOS cannot bind a client socket to 127.0.0.6.
        final Socket probe = new Socket();
        try {
            probe.bind(new InetSocketAddress("127.0.0.6", 0));
        } catch (IOException e) {
            assumeTrue(false, "cannot bind 127.0.0.6 on this platform");
        } finally {
            probe.close();
        }

        final VertxTcpServer server = plainServer(1, ConnectionLimiter::isLoopback, 0);
        final int port = listen(server);

        final Socket counted = connectFrom("127.0.0.6", port);
        assertThat(readGreeting(counted)).isEqualTo("OK");
        assertThat(server.currentConnections()).isEqualTo(1);

        assertThat(readUntilEnd(connectFrom("127.0.0.6", port))).isEmpty();
        assertThat(readGreeting(connect(port))).as("own probe from 127.0.0.1 is still served").isEqualTo("OK");
    }

    // ---- AC5: PROXY protocol

    @Test
    void should_apply_the_limit_once_the_proxy_header_is_read_and_close_a_socket_that_never_sends_it() throws Exception {
        final VertxTcpServer server = plainServer(1, NOBODY_EXEMPT, 0, true);
        final int port = listen(server);

        final Socket first = connect(port);
        first.getOutputStream().write("PROXY TCP4 203.0.113.9 127.0.0.1 40000 1234\r\n".getBytes(StandardCharsets.US_ASCII));
        assertThat(readGreeting(first)).isEqualTo("OK");

        final Socket second = connect(port);
        second.getOutputStream().write("PROXY TCP4 203.0.113.10 127.0.0.1 40001 1234\r\n".getBytes(StandardCharsets.US_ASCII));
        assertThat(readUntilEnd(second)).as("bytes received by a refused client").isEmpty();

        final Socket silent = connect(port);
        silent.setSoTimeout(5000);
        final long begin = System.nanoTime();
        assertThat(silent.getInputStream().read()).as("closed by the proxy protocol timeout").isEqualTo(-1);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin)).isLessThan(3500);
        assertThat(server.currentConnections()).isEqualTo(1);
    }

    // ---- AC7: no limit, today's accept path

    @Test
    void should_use_the_unwrapped_net_server_when_no_limit_is_set() throws Exception {
        final VertxTcpServer server = plainServer(0, NOBODY_EXEMPT, 0);
        final NetServer netServer = server.newInstance();
        assertThat(netServer).isNotInstanceOf(LimitedNetServer.class);
        assertThat(server.currentConnections()).isZero();
        final int port = listen(netServer);
        for (int i = 0; i < 4; i++) {
            assertThat(readGreeting(connect(port))).isEqualTo("OK");
        }
    }

    @Test
    void should_reject_a_negative_limit_instead_of_silently_running_unlimited() {
        assertThatThrownBy(() -> plainServer(-1, NOBODY_EXEMPT, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---- loopback exemption is a builder option

    @Test
    void should_exempt_loopback_by_default_and_let_the_caller_turn_it_off() throws Exception {
        assertThat(VertxTcpServerOptions.builder().build().isExemptLoopback()).isTrue();
        assertThat(VertxTcpServerOptions.builder().exemptLoopback(false).build().isExemptLoopback()).isFalse();

        // through the public constructor: the connections of this test come from 127.0.0.1
        final VertxTcpServer exempting = publicServer(1, true);
        final int exemptingPort = listen(exempting);
        for (int i = 0; i < 3; i++) {
            assertThat(readGreeting(connect(exemptingPort))).isEqualTo("OK");
        }
        assertThat(exempting.currentConnections()).isZero();

        final VertxTcpServer counting = publicServer(1, false);
        final int countingPort = listen(counting);
        assertThat(readGreeting(connect(countingPort))).isEqualTo("OK");
        assertThat(readUntilEnd(connect(countingPort))).as("bytes received by a refused client").isEmpty();
        assertThat(counting.currentConnections()).isEqualTo(1);
    }

    @Test
    void should_judge_the_exemption_on_the_real_peer_and_not_on_the_address_declared_by_the_proxy_header() throws Exception {
        final List<String> judged = new CopyOnWriteArrayList<>();
        final VertxTcpServer server = plainServer(
            2,
            address -> {
                judged.add(address.host());
                return false;
            },
            0,
            true
        );
        final int port = listen(server);

        final Socket client = connect(port);
        client.getOutputStream().write("PROXY TCP4 127.0.0.1 127.0.0.1 40000 1234\r\n".getBytes(StandardCharsets.US_ASCII));
        assertThat(readGreeting(client)).isEqualTo("OK");
        final Socket spoofing = connect(port);
        spoofing.getOutputStream().write("PROXY TCP4 203.0.113.9 127.0.0.1 40001 1234\r\n".getBytes(StandardCharsets.US_ASCII));
        assertThat(readGreeting(spoofing)).isEqualTo("OK");

        assertThat(judged).as("addresses handed to the exemption").hasSize(2).containsOnly("127.0.0.1");
        assertThat(server.currentConnections()).isEqualTo(2);
    }

    // ---- AC8: builder option only, no configuration key

    @Test
    void should_expose_max_connections_on_the_tcp_options_builder_only() {
        assertThat(VertxTcpServerOptions.builder().build().getMaxConnections()).isZero();
        assertThat(VertxTcpServerOptions.builder().maxConnections(42).build().getMaxConnections()).isEqualTo(42);
        assertThatThrownBy(() -> VertxServerOptions.class.getDeclaredField("maxConnections")).isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    void should_not_read_any_configuration_key_for_the_limit() {
        final MockEnvironment environment = new MockEnvironment()
            .withProperty("tcp.type", "tcp")
            .withProperty("tcp.maxConnections", "3")
            .withProperty("tcp.max-connections", "3")
            .withProperty("tcp.maxConnectionsPerGiB", "3");

        final var options = VertxServerOptions.builder(environment, "tcp").build();

        assertThat(options).isInstanceOf(VertxTcpServerOptions.class);
        assertThat(((VertxTcpServerOptions) options).getMaxConnections()).isZero();
    }

    // ---- AC9: open connections are never closed by the limit

    @Test
    void should_keep_open_connections_and_admit_again_when_the_count_drops() throws Exception {
        final VertxTcpServer server = plainServer(2, NOBODY_EXEMPT, 0, socket -> {
            socket.write("OK\n");
            socket.handler(buffer -> socket.write(buffer));
        });
        final int port = listen(server);

        final Socket a = connect(port);
        final Socket b = connect(port);
        assertThat(readGreeting(a)).isEqualTo("OK");
        assertThat(readGreeting(b)).isEqualTo("OK");

        assertThat(readUntilEnd(connect(port))).isEmpty(); // refused: limit reached

        assertThat(echo(a, "ping-a")).isEqualTo("ping-a");
        assertThat(echo(b, "ping-b")).isEqualTo("ping-b");

        a.close();
        waitUntil(() -> server.currentConnections() == 1);
        final Socket again = connect(port);
        assertThat(readGreeting(again)).isEqualTo("OK");
        assertThat(echo(b, "still-here")).isEqualTo("still-here");
    }

    // ---- AC10: one count across the listeners

    @Test
    void should_share_one_count_across_the_net_servers_of_a_server() throws Exception {
        final VertxTcpServer server = plainServer(3, NOBODY_EXEMPT, 0);
        final int portA = listen(server);
        final int portB = listen(server);

        final Socket a1 = connect(portA);
        final Socket a2 = connect(portA);
        final Socket b1 = connect(portB);
        assertThat(readGreeting(a1)).isEqualTo("OK");
        assertThat(readGreeting(a2)).isEqualTo("OK");
        assertThat(readGreeting(b1)).isEqualTo("OK");
        assertThat(server.currentConnections()).isEqualTo(3);

        assertThat(readUntilEnd(connect(portA))).isEmpty();
        assertThat(readUntilEnd(connect(portB))).isEmpty();

        a1.close();
        waitUntil(() -> server.currentConnections() == 2);
        assertThat(readGreeting(connect(portB))).as("a slot freed on A admits on B").isEqualTo("OK");
    }

    // ---- TLS server with a limit: refusal before any TLS byte, handshake at the accepted socket

    @Test
    void should_close_without_any_tls_byte_a_connection_past_the_limit_on_a_tls_server() throws Exception {
        final TlsFixture tls = new TlsFixture();
        final VertxTcpServer server = tlsServer(tls, 1);
        final int port = listen(server);

        final SSLSocket first = tls.handshake(connect(port), port);
        assertThat(first.getSession().isValid()).isTrue();
        assertThat(readGreeting(first)).isEqualTo("OK");

        final Socket refused = connect(port);
        // what a ClientHello looks like on the wire: a TLS server would answer with a handshake or an alert
        refused.getOutputStream().write(new byte[] { 0x16, 0x03, 0x01, 0x00, 0x05, 0x01, 0x00, 0x00, 0x01, 0x00 });
        assertThat(readUntilEnd(refused)).as("bytes received by a refused TLS client").isEmpty();
        assertThat(server.currentConnections()).isEqualTo(1);
    }

    @Test
    void should_complete_the_tls_handshake_after_a_proxy_header_on_a_limited_server() throws Exception {
        final TlsFixture tls = new TlsFixture();
        final VertxTcpServer server = tlsServer(tls, 1, true);
        final int port = listen(server);

        // the header and the first bytes of the ClientHello travel in the same TCP segment
        final Socket raw = new ProxyHeaderSocket("127.0.0.1", port);
        sockets.add(raw);
        final SSLSocket client = tls.handshake(raw, port);
        assertThat(client.getSession().isValid()).isTrue();
        assertThat(readGreeting(client)).isEqualTo("OK");
        assertThat(server.currentConnections()).isEqualTo(1);
        client.close();
        waitUntil(() -> server.currentConnections() == 0);
    }

    @Test
    void should_close_and_release_the_slot_of_an_admitted_client_that_sends_garbage_instead_of_a_client_hello() throws Exception {
        final TlsFixture tls = new TlsFixture();
        final VertxTcpServer server = tlsServer(tls, 1);
        final int port = listen(server);

        final Socket garbage = connect(port);
        garbage.getOutputStream().write("GET / HTTP/1.1\r\nHost: a.example.test\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        try {
            readUntilEnd(garbage); // closed by the server, possibly after a TLS alert
        } catch (SocketException reset) {
            // closing with unread bytes may reset the connection: the slot is what matters here
        }

        waitUntil(() -> server.currentConnections() == 0);
        assertThat(readGreeting(tls.handshake(connect(port), port))).as("the slot is usable again").isEqualTo("OK");
    }

    @Test
    void should_serve_the_configured_sni_certificate_below_the_limit_and_release_the_slot() throws Exception {
        final TlsFixture tls = new TlsFixture();
        final VertxTcpServer server = tlsServer(tls, 1);
        final int port = listen(server);

        final SSLSocket client = tls.handshake(connect(port), port);
        assertThat(readGreeting(client)).isEqualTo("OK");
        assertThat(server.currentConnections()).isEqualTo(1);
        client.close();
        waitUntil(() -> server.currentConnections() == 0);
    }

    @Test
    void should_never_lose_the_client_hello_on_a_fresh_server() throws Exception {
        final TlsFixture tls = new TlsFixture();
        int failures = 0;
        final int rounds = 200;
        for (int i = 0; i < rounds; i++) {
            // a fresh server each time: the first connection is where the ClientHello used to be lost
            final VertxTcpServer server = tlsServer(tls, 100);
            final int port = listen(server);
            try {
                final SSLSocket client = tls.handshake(connect(port), port);
                if (!"OK".equals(readGreeting(client))) {
                    failures++;
                }
                client.close();
            } catch (IOException e) {
                failures++;
            }
        }
        assertThat(failures).as("failed first handshakes out of " + rounds).isZero();
    }

    // ---- helpers

    private VertxTcpServer plainServer(int limit, Predicate<SocketAddress> exempt, int idleTimeoutSeconds) {
        return plainServer(limit, exempt, idleTimeoutSeconds, false);
    }

    private VertxTcpServer plainServer(int limit, Predicate<SocketAddress> exempt, int idleTimeoutSeconds, boolean proxyProtocol) {
        return plainServer(limit, exempt, idleTimeoutSeconds, proxyProtocol, null);
    }

    private VertxTcpServer plainServer(int limit, Predicate<SocketAddress> exempt, int idleTimeoutSeconds, Consumer<NetSocket> handler) {
        return plainServer(limit, exempt, idleTimeoutSeconds, false, handler);
    }

    private VertxTcpServer plainServer(
        int limit,
        Predicate<SocketAddress> exempt,
        int idleTimeoutSeconds,
        boolean proxyProtocol,
        Consumer<NetSocket> handler
    ) {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder()
            .id("limited")
            .host("127.0.0.1")
            .port(0)
            .maxConnections(limit)
            .idleTimeout(idleTimeoutSeconds)
            .haProxyProtocol(proxyProtocol)
            .haProxyProtocolTimeout(1) // seconds
            .build();
        final KeyStoreLoaderManager keyStoreLoaderManager = Mockito.mock(KeyStoreLoaderManager.class);
        Mockito.when(keyStoreLoaderManager.getKeyManager()).thenReturn(Mockito.mock(X509KeyManager.class));
        final TrustStoreLoaderManager trustStoreLoaderManager = Mockito.mock(TrustStoreLoaderManager.class);
        Mockito.when(trustStoreLoaderManager.getCertificateManager()).thenReturn(Mockito.mock(X509TrustManager.class));
        final VertxTcpServer server = new VertxTcpServer(
            vertx,
            options,
            keyStoreLoaderManager,
            trustStoreLoaderManager,
            Mockito.mock(CRLLoaderManager.class),
            exempt
        );
        appHandler = handler;
        return server;
    }

    private Consumer<NetSocket> appHandler;

    /** Creates a listener on the server, wired with the greeting handler, and returns its port. */
    private int listen(VertxTcpServer server) {
        return listen(server.newInstance(), server);
    }

    private int listen(NetServer netServer) {
        return listen(netServer, null);
    }

    private int listen(NetServer netServer, VertxTcpServer server) {
        final var handler = appHandler;
        netServer.connectHandler(socket -> {
            admitted.incrementAndGet();
            if (server != null) {
                maxSeenAtAccept.accumulateAndGet(server.currentConnections(), Math::max);
            }
            if (handler != null) {
                handler.accept(socket);
            } else {
                socket.write("OK\n");
            }
        });
        netServer.listen(0, "127.0.0.1").timeout(10, TimeUnit.SECONDS).blockingGet();
        netServers.add(netServer);
        return netServer.actualPort();
    }

    private Socket connect(int port) throws IOException {
        final Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
        socket.setSoTimeout(5000);
        sockets.add(socket);
        return socket;
    }

    private Socket connectFrom(String sourceAddress, int port) throws IOException {
        final Socket socket = new Socket();
        socket.bind(new InetSocketAddress(sourceAddress, 0));
        socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
        socket.setSoTimeout(5000);
        sockets.add(socket);
        return socket;
    }

    private static String readGreeting(Socket socket) throws IOException {
        return readGreeting(socket.getInputStream());
    }

    private static String readGreeting(InputStream in) throws IOException {
        final StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && c != '\n') {
            sb.append((char) c);
        }
        return sb.toString();
    }

    /** Reads until the server closes with FIN. An RST (SocketException) or a read timeout fails the test. */
    private static String readUntilEnd(Socket socket) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final InputStream in = socket.getInputStream();
        try {
            int c;
            while ((c = in.read()) != -1) {
                sb.append((char) c);
            }
        } catch (SocketTimeoutException e) {
            throw new AssertionError("connection was not closed by the server", e);
        }
        return sb.toString();
    }

    private static String echo(Socket socket, String message) throws IOException {
        socket.getOutputStream().write(message.getBytes(StandardCharsets.US_ASCII));
        final byte[] buffer = new byte[message.length()];
        int read = 0;
        while (read < buffer.length) {
            final int n = socket.getInputStream().read(buffer, read, buffer.length - read);
            if (n < 0) {
                throw new IOException("closed after " + read + " bytes");
            }
            read += n;
        }
        return new String(buffer, StandardCharsets.US_ASCII);
    }

    private static void waitUntil(BooleanSupplier condition) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(20)).until(condition::getAsBoolean);
    }

    /** A client socket that prepends a PROXY protocol v1 header to its first write, in the same TCP segment. */
    private static final class ProxyHeaderSocket extends Socket {

        private static final byte[] HEADER = "PROXY TCP4 203.0.113.9 127.0.0.1 40000 1234\r\n".getBytes(StandardCharsets.US_ASCII);

        private boolean headerSent;

        ProxyHeaderSocket(String host, int port) throws IOException {
            super(host, port);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new HeaderPrependingStream(super.getOutputStream());
        }

        private final class HeaderPrependingStream extends FilterOutputStream {

            HeaderPrependingStream(OutputStream out) {
                super(out);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (headerSent) {
                    out.write(b, off, len);
                    return;
                }
                headerSent = true;
                final byte[] all = new byte[HEADER.length + len];
                System.arraycopy(HEADER, 0, all, 0, HEADER.length);
                System.arraycopy(b, off, all, HEADER.length, len);
                out.write(all, 0, all.length);
            }
        }
    }

    // ---- TLS fixture

    private final class TlsFixture {

        final TestPki.Ident ca;
        final Path keyStore;
        final Path trustStore;
        final SSLContext clientContext;

        TlsFixture() throws Exception {
            ca = TestPki.ca("test-ca");
            final TestPki.Ident leaf = TestPki.leaf(ca, "server", "a.example.test");
            keyStore = tmp.resolve("server-" + System.nanoTime() + ".p12");
            trustStore = tmp.resolve("trust-" + System.nanoTime() + ".p12");
            TestPki.writeKeyStore(keyStore, ca, leaf);
            TestPki.writeTrustStore(trustStore, ca);

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(TestPki.trustOf(ca));
            clientContext = SSLContext.getInstance("TLS");
            clientContext.init(null, tmf.getTrustManagers(), null);
        }

        /** Starts the handshake at once, as a real TLS client does. */
        SSLSocket handshake(Socket raw, int port) throws IOException {
            final SSLSocket socket = (SSLSocket) clientContext.getSocketFactory().createSocket(raw, "127.0.0.1", port, true);
            final SSLParameters parameters = socket.getSSLParameters();
            parameters.setServerNames(List.of(new SNIHostName("a.example.test")));
            socket.setSSLParameters(parameters);
            socket.startHandshake();
            return socket;
        }
    }

    /** A limited server built through the public constructor, i.e. with the exemption derived from the options. */
    private VertxTcpServer publicServer(int limit, boolean exemptLoopback) {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder()
            .id("limited")
            .host("127.0.0.1")
            .port(0)
            .maxConnections(limit)
            .exemptLoopback(exemptLoopback)
            .build();
        final KeyStoreLoaderManager keyStoreLoaderManager = Mockito.mock(KeyStoreLoaderManager.class);
        Mockito.when(keyStoreLoaderManager.getKeyManager()).thenReturn(Mockito.mock(X509KeyManager.class));
        final TrustStoreLoaderManager trustStoreLoaderManager = Mockito.mock(TrustStoreLoaderManager.class);
        Mockito.when(trustStoreLoaderManager.getCertificateManager()).thenReturn(Mockito.mock(X509TrustManager.class));
        appHandler = null;
        return new VertxTcpServer(vertx, options, keyStoreLoaderManager, trustStoreLoaderManager, Mockito.mock(CRLLoaderManager.class));
    }

    private VertxTcpServer tlsServer(TlsFixture tls, int limit) {
        return tlsServer(tls, limit, false);
    }

    private VertxTcpServer tlsServer(TlsFixture tls, int limit, boolean proxyProtocol) {
        final var keyStores = new DefaultKeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions>();
        keyStores.registerFactory(new FileKeyStoreLoaderFactory());
        final var trustStores = new DefaultKeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions>();
        trustStores.registerFactory(new FileTrustStoreLoaderFactory());
        final var crls = new DefaultCRLLoaderFactoryRegistry();
        crls.registerFactory(new FileCRLLoaderFactory());

        final VertxTcpServerOptions options = VertxTcpServerOptions.builder()
            .id("limited-tls")
            .prefix("limited-tls")
            .environment(new MockEnvironment())
            .host("127.0.0.1")
            .port(0)
            .secured(true)
            .sni(true)
            .maxConnections(limit)
            .haProxyProtocol(proxyProtocol)
            .haProxyProtocolTimeout(1)
            .keyStoreLoaderOptions(
                KeyStoreLoaderOptions.builder()
                    .type("PKCS12")
                    .paths(List.of(tls.keyStore.toString()))
                    .password("secret")
                    .watch(false)
                    .defaultAlias("server")
                    .build()
            )
            .trustStoreLoaderOptions(
                TrustStoreLoaderOptions.builder()
                    .type("PKCS12")
                    .paths(List.of(tls.trustStore.toString()))
                    .password("secret")
                    .watch(false)
                    .build()
            )
            .crlLoaderOptions(CRLLoaderOptions.builder().watch(false).build())
            .build();

        final VertxTcpServer server = new VertxTcpServerFactory(vertx, keyStores, trustStores, crls).create(options);
        // the tests connect from loopback, which is exempt by default: exempt nobody
        server.exemptConnections(NOBODY_EXEMPT);
        appHandler = null;
        return server;
    }
}
