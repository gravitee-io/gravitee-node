package io.gravitee.node.vertx.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.node.vertx.client.http.VertxWebSocketClientFactory;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.ssl.pem.PEMTrustStore;
import io.gravitee.node.vertx.client.tcp.VertxTcpClientFactory;
import io.netty.handler.ssl.OpenSsl;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.impl.CleanableHttpClient;
import io.vertx.core.http.impl.CleanableWebSocketClient;
import io.vertx.core.http.impl.WebSocketClientImpl;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.lang.reflect.Field;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.X509ExtendedTrustManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

/**
 * With {@code trustAll}, any certificate is accepted, so verifying its hostname adds nothing. Netty 4.2.17+ also refuses
 * the handshake on Java 25 with OpenSSL when Vert.x combines both, so the client factories must turn the verification off.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrustAllHostnameVerificationTest {

    private final Vertx vertx = Vertx.vertx();

    @Mock
    private Configuration nodeConfiguration;

    @BeforeEach
    void setUp() {
        lenient().when(nodeConfiguration.getProperty(anyString(), eq(Boolean.class), any())).thenReturn(false);
    }

    @AfterAll
    void tearDown() {
        vertx.close().blockingAwait(10, TimeUnit.SECONDS);
    }

    @Nested
    class Options {

        @Test
        void http_client_does_not_verify_host_with_trust_all() throws Exception {
            // Given / When
            HttpClientOptions options = httpClientOptions(trustAll());

            // Then
            assertThat(options.isVerifyHost()).isFalse();
            assertTrustsAllWithExtendedTrustManager(options);
        }

        @Test
        void http_client_verifies_host_without_trust_all() {
            // Given / When
            HttpClientOptions options = httpClientOptions(truststore());

            // Then
            assertThat(options.isVerifyHost()).isTrue();
        }

        @Test
        void websocket_client_does_not_verify_host_with_trust_all() throws Exception {
            // Given / When
            WebSocketClientOptions options = webSocketClientOptions(trustAll());

            // Then
            assertThat(options.isVerifyHost()).isFalse();
            assertTrustsAllWithExtendedTrustManager(options);
        }

        @Test
        void websocket_client_verifies_host_without_trust_all() throws Exception {
            // Given / When
            WebSocketClientOptions options = webSocketClientOptions(truststore());

            // Then
            assertThat(options.isVerifyHost()).isTrue();
        }

        @Test
        void tcp_client_does_not_verify_host_with_trust_all() throws Exception {
            // Given
            SslOptions sslOptions = trustAll();
            sslOptions.setHostnameVerificationAlgorithm("HTTPS");
            NetClientOptions options = new NetClientOptions();

            // When
            VertxTcpClientFactory.configureSslClientOption(options, sslOptions);

            // Then
            assertThat(options.getHostnameVerificationAlgorithm()).isEmpty();
            assertTrustsAllWithExtendedTrustManager(options);
        }

        @Test
        void tcp_client_keeps_algorithm_without_trust_all() throws Exception {
            // Given
            SslOptions sslOptions = truststore();
            sslOptions.setHostnameVerificationAlgorithm("HTTPS");
            NetClientOptions options = new NetClientOptions();

            // When
            VertxTcpClientFactory.configureSslClientOption(options, sslOptions);

            // Then
            assertThat(options.getHostnameVerificationAlgorithm()).isEqualTo("HTTPS");
        }

        @Test
        void effective_algorithm_is_empty_with_trust_all_even_when_explicit() {
            // Given
            SslOptions sslOptions = SslOptions.builder()
                .trustAll(true)
                .hostnameVerifier(true)
                .hostnameVerificationAlgorithm("HTTPS")
                .build();

            // When / Then
            assertThat(sslOptions.effectiveHostnameVerificationAlgorithm()).isEmpty();
            assertThat(sslOptions.effectiveVerifyHost()).isFalse();
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Handshake {

        private HttpServer server;
        private int port;

        @BeforeAll
        void startServer() {
            server = vertx
                .createHttpServer(
                    new HttpServerOptions()
                        .setSsl(true)
                        .setKeyCertOptions(
                            new PemKeyCertOptions().setCertPath(sslFile("server-localhost.crt")).setKeyPath(sslFile("server-localhost.key"))
                        )
                )
                .requestHandler(request -> request.response().rxEnd("ok").subscribe())
                .rxListen(0)
                .blockingGet();
            port = server.actualPort();
        }

        @AfterAll
        void stopServer() {
            server.rxClose().blockingAwait(10, TimeUnit.SECONDS);
        }

        @ParameterizedTest(name = "openssl={0}")
        @ValueSource(booleans = { false, true })
        void trust_all_connects_to_a_host_absent_from_the_certificate(boolean openssl) {
            // Given: the certificate is issued for "localhost" only
            SslOptions sslOptions = trustAll();

            // When
            String body = get(sslOptions, "127.0.0.1", openssl);

            // Then
            assertThat(body).isEqualTo("ok");
        }

        @ParameterizedTest(name = "openssl={0}")
        @ValueSource(booleans = { false, true })
        void truststore_connects_to_the_certified_host(boolean openssl) {
            // Given
            SslOptions sslOptions = truststore();

            // When
            String body = get(sslOptions, "localhost", openssl);

            // Then
            assertThat(body).isEqualTo("ok");
        }

        @ParameterizedTest(name = "openssl={0}")
        @ValueSource(booleans = { false, true })
        void truststore_rejects_a_host_absent_from_the_certificate(boolean openssl) {
            // Given: the server certificate is trusted, so only the hostname check can fail
            SslOptions sslOptions = truststore();

            // When / Then
            assertThatThrownBy(() -> get(sslOptions, "127.0.0.1", openssl))
                .rootCause()
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("No subject alternative names matching IP address 127.0.0.1");
        }

        private String get(SslOptions sslOptions, String host, boolean openssl) {
            assumeTrue(!openssl || OpenSsl.isAvailable(), "OpenSSL is not available");
            lenient().when(nodeConfiguration.getProperty(eq("http.ssl.openssl"), eq(Boolean.class), any())).thenReturn(openssl);
            HttpClient client = httpClient(sslOptions, "https://" + host + ":" + port);
            try {
                return client
                    .rxRequest(HttpMethod.GET, port, host, "/")
                    .flatMap(request -> request.rxSend())
                    .flatMap(HttpClientResponse::rxBody)
                    .blockingGet()
                    .toString();
            } finally {
                client.rxClose().blockingAwait(10, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * The Vert.x {@code trustAll} flag must stay off: its trust manager is not an X509ExtendedTrustManager, which
     * Netty 4.2.17+ refuses with OpenSSL.
     */
    private void assertTrustsAllWithExtendedTrustManager(ClientOptionsBase options) throws Exception {
        assertThat(options.isTrustAll()).isFalse();
        assertThat(options.getTrustOptions().getTrustManagerFactory(vertx.getDelegate()).getTrustManagers())
            .singleElement()
            .isInstanceOf(X509ExtendedTrustManager.class);
    }

    private static SslOptions trustAll() {
        return SslOptions.builder().trustAll(true).hostnameVerifier(true).build();
    }

    private static SslOptions truststore() {
        PEMTrustStore trustStore = new PEMTrustStore();
        trustStore.setPath(sslFile("server-localhost.crt"));
        return SslOptions.builder().trustAll(false).hostnameVerifier(true).trustStore(trustStore).build();
    }

    private HttpClient httpClient(SslOptions sslOptions, String target) {
        return VertxHttpClientFactory.builder()
            .vertx(vertx)
            .nodeConfiguration(nodeConfiguration)
            .defaultTarget(target)
            .sslOptions(sslOptions)
            .build()
            .createHttpClient();
    }

    private HttpClientOptions httpClientOptions(SslOptions sslOptions) {
        HttpClient client = httpClient(sslOptions, "https://localhost:8443");
        try {
            return ((CleanableHttpClient) client.getDelegate()).options();
        } finally {
            client.close();
        }
    }

    private WebSocketClientOptions webSocketClientOptions(SslOptions sslOptions) throws IllegalAccessException {
        WebSocketClient client = VertxWebSocketClientFactory.builder()
            .vertx(vertx)
            .nodeConfiguration(nodeConfiguration)
            .defaultTarget("wss://localhost:8443")
            .sslOptions(sslOptions)
            .build()
            .createWebSocketClient();
        try {
            Field delegate = ReflectionUtils.findField(
                CleanableWebSocketClient.class,
                "delegate",
                io.vertx.core.http.WebSocketClient.class
            );
            delegate.setAccessible(true);
            WebSocketClientImpl impl = (WebSocketClientImpl) delegate.get(client.getDelegate());
            Field options = ReflectionUtils.findField(WebSocketClientImpl.class, "options", WebSocketClientOptions.class);
            options.setAccessible(true);
            return (WebSocketClientOptions) options.get(impl);
        } finally {
            client.close();
        }
    }

    private static String sslFile(String file) {
        return Objects.requireNonNull(TrustAllHostnameVerificationTest.class.getResource("/ssl/" + file)).getPath();
    }
}
