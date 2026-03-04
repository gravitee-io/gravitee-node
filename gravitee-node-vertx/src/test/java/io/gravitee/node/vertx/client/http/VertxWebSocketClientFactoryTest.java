package io.gravitee.node.vertx.client.http;

import static io.gravitee.node.vertx.client.http.VertxHttpClientFactory.HTTP_SSL_OPENSSL_CONFIGURATION;
import static io.gravitee.node.vertx.client.http.VertxHttpProxyType.HTTP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.ssl.jks.JKSKeyStore;
import io.gravitee.node.vertx.client.ssl.jks.JKSTrustStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMKeyStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMTrustStore;
import io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12TrustStore;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.impl.CleanableWebSocketClient;
import io.vertx.core.http.impl.WebSocketClientImpl;
import io.vertx.core.net.ProxyType;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Objects;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxWebSocketClientFactoryTest {

    protected static final String PASSWORD = "gravitee";
    private final Vertx vertx = Vertx.vertx();

    @Mock
    private Configuration nodeConfiguration;

    private VertxWebSocketClientFactory.VertxWebSocketClientFactoryBuilder builder() {
        final VertxHttpClientOptions httpOptions = VertxHttpClientOptions
            .builder()
            .keepAlive(true)
            .readTimeout(10000)
            .idleTimeout(60000)
            .keepAliveTimeout(30000)
            .connectTimeout(5000)
            .useCompression(false)
            .maxConcurrentConnections(100)
            .pipelining(false)
            .build();
        final VertxHttpProxyOptions proxyOptions = VertxHttpProxyOptions
            .builder()
            .enabled(true)
            .useSystemProxy(false)
            .host("localhost")
            .port(8080)
            .username("user")
            .password("pwd")
            .type(HTTP)
            .build();

        when(nodeConfiguration.getProperty(HTTP_SSL_OPENSSL_CONFIGURATION, Boolean.class, false)).thenReturn(false);

        return VertxWebSocketClientFactory
            .builder()
            .vertx(vertx)
            .nodeConfiguration(nodeConfiguration)
            .name("test")
            .shared(false)
            .defaultTarget("https://api.gravitee.io/echo")
            .httpOptions(httpOptions)
            .proxyOptions(proxyOptions);
    }

    @Test
    @SneakyThrows
    void should_build_websocket_client_with_default_settings() {
        final WebSocketClient webSocketClient = builder()
            .httpOptions(VertxHttpClientOptions.builder().build())
            .build()
            .createWebSocketClient();

        assertThat(webSocketClient).isNotNull();
        final WebSocketClientOptions options = extractWebSocketClientOptions(webSocketClient);
        assertThat(options.getTryUsePerFrameCompression()).isTrue();
        assertThat(options.getTryUsePerMessageCompression()).isTrue();
        assertThat(options.getCompressionAllowClientNoContext()).isTrue();
        assertThat(options.getCompressionRequestServerNoContext()).isTrue();
    }

    @Test
    @SneakyThrows
    void should_build_websocket_client_with_compression_disabled() {
        final WebSocketClient webSocketClient = builder()
            .httpOptions(VertxHttpClientOptions.builder().useCompression(false).build())
            .build()
            .createWebSocketClient();

        assertThat(webSocketClient).isNotNull();
        final WebSocketClientOptions options = extractWebSocketClientOptions(webSocketClient);
        assertThat(options.getTryUsePerFrameCompression()).isFalse();
        assertThat(options.getTryUsePerMessageCompression()).isFalse();
        assertThat(options.getCompressionAllowClientNoContext()).isFalse();
        assertThat(options.getCompressionRequestServerNoContext()).isFalse();
    }

    @Test
    @SneakyThrows
    void should_build_websocket_client_with_custom_client_options() {
        final VertxHttpClientOptions httpOptions = VertxHttpClientOptions
            .builder()
            .idleTimeout(30000)
            .connectTimeout(3000)
            .maxConcurrentConnections(50)
            .build();

        final WebSocketClient webSocketClient = builder().httpOptions(httpOptions).build().createWebSocketClient();

        assertThat(webSocketClient).isNotNull();
        final WebSocketClientOptions options = extractWebSocketClientOptions(webSocketClient);
        assertThat(options.getIdleTimeout()).isEqualTo(30);
        assertThat(options.getConnectTimeout()).isEqualTo(3000);
        assertThat(options.getMaxConnections()).isEqualTo(50);
    }

    @Test
    @SneakyThrows
    void should_build_websocket_client_with_custom_proxy() {
        final VertxHttpProxyOptions proxyOptions = VertxHttpProxyOptions
            .builder()
            .enabled(true)
            .useSystemProxy(false)
            .host("proxy.example.com")
            .port(3128)
            .username("proxyuser")
            .password("proxypwd")
            .type(HTTP)
            .build();

        final WebSocketClient webSocketClient = builder().proxyOptions(proxyOptions).build().createWebSocketClient();

        assertThat(webSocketClient).isNotNull();
        final WebSocketClientOptions options = extractWebSocketClientOptions(webSocketClient);
        assertThat(options.getProxyOptions()).isNotNull();
        assertThat(options.getProxyOptions().getHost()).isEqualTo("proxy.example.com");
        assertThat(options.getProxyOptions().getPort()).isEqualTo(3128);
        assertThat(options.getProxyOptions().getType()).isEqualTo(ProxyType.HTTP);
    }

    @Test
    void should_build_websocket_client_with_system_proxy() {
        final VertxHttpProxyOptions proxyOptions = VertxHttpProxyOptions
            .builder()
            .enabled(true)
            .useSystemProxy(true)
            .host("localhost")
            .port(8080)
            .username("user")
            .password("pwd")
            .type(HTTP)
            .build();

        final WebSocketClient webSocketClient = builder().proxyOptions(proxyOptions).build().createWebSocketClient();

        assertThat(webSocketClient).isNotNull();
    }

    @Nested
    class PEM {

        @Test
        void should_throw_illegal_argument_exception_with_PEM_trustStore_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(new PEMTrustStore());

            final VertxWebSocketClientFactory factory = builder().sslOptions(sslOptions).build();
            assertThrows(IllegalArgumentException.class, factory::createWebSocketClient);
        }

        @Test
        void should_build_websocket_client_with_PEM_trustStore_path() {
            final PEMTrustStore trustStore = new PEMTrustStore();
            trustStore.setPath(getSslFilePath("truststore.pem"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(trustStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_build_websocket_client_with_PEM_trustStore_content() throws Exception {
            final PEMTrustStore trustStore = new PEMTrustStore();
            trustStore.setContent(getSslFileContent("truststore.pem"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(trustStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_throw_illegal_argument_exception_with_PEM_cert_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(new PEMKeyStore());

            final VertxWebSocketClientFactory factory = builder().sslOptions(sslOptions).build();
            assertThrows(IllegalArgumentException.class, factory::createWebSocketClient);
        }

        @Test
        void should_throw_illegal_argument_exception_with_pem_key_missing_path_or_content() {
            final PEMKeyStore keyStore = new PEMKeyStore();
            keyStore.setCertPath(getSslFilePath("client.cer"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(keyStore);

            final VertxWebSocketClientFactory factory = builder().sslOptions(sslOptions).build();
            assertThrows(IllegalArgumentException.class, factory::createWebSocketClient);
        }

        @Test
        void should_build_websocket_client_with_pem_key_store_path() {
            final PEMKeyStore keyStore = new PEMKeyStore();
            keyStore.setKeyPath(getSslFilePath("client.key"));
            keyStore.setCertPath(getSslFilePath("client.cer"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(keyStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_build_websocket_client_with_pem_keystore_content() throws Exception {
            final PEMKeyStore keystore = new PEMKeyStore();
            keystore.setKeyContent(getSslFileContent("client.key"));
            keystore.setCertContent(getSslFileContent("client.cer"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(keystore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }
    }

    @Nested
    class PKCS12 {

        @Test
        void should_throw_illegal_argument_exception_with_pkcs12_trust_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(new PKCS12TrustStore());

            final VertxWebSocketClientFactory factory = builder().sslOptions(sslOptions).build();
            assertThrows(IllegalArgumentException.class, factory::createWebSocketClient);
        }

        @Test
        void should_build_websocket_client_with_pkcs12_trust_store_path() {
            final PKCS12TrustStore trustStore = new PKCS12TrustStore();
            trustStore.setPath(getSslFilePath("truststore.p12"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(trustStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_build_websocket_client_with_pkcs12_trust_store_content() throws Exception {
            final PKCS12TrustStore trustStore = new PKCS12TrustStore();
            trustStore.setContent(getContentAsBase64("truststore.p12"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(trustStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_throw_illegal_argument_exception_with_pkcs12_key_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(new PKCS12KeyStore());

            final VertxWebSocketClientFactory factory = builder().sslOptions(sslOptions).build();
            assertThrows(IllegalArgumentException.class, factory::createWebSocketClient);
        }

        @Test
        void should_build_websocket_client_with_pkcs12_key_store_path() {
            final PKCS12KeyStore keyStore = new PKCS12KeyStore();
            keyStore.setPath(getSslFilePath("client.p12"));
            keyStore.setPassword(PASSWORD);

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(keyStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_build_websocket_client_with_pkcs12_keystore_content() throws Exception {
            final PKCS12KeyStore keystore = new PKCS12KeyStore();
            keystore.setContent(getContentAsBase64("client.p12"));
            keystore.setPassword(PASSWORD);

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(keystore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }
    }

    @Nested
    class JKS {

        @Test
        void should_throw_illegal_argument_exception_with_jks_trust_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(new JKSTrustStore());

            final VertxWebSocketClientFactory factory = builder().sslOptions(sslOptions).build();
            assertThrows(IllegalArgumentException.class, factory::createWebSocketClient);
        }

        @Test
        void should_build_websocket_client_with_jks_trust_store_path() {
            final JKSTrustStore trustStore = new JKSTrustStore();
            trustStore.setPath(getSslFilePath("truststore.jks"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(trustStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_build_websocket_client_with_jks_trust_store_content() throws Exception {
            final JKSTrustStore trustStore = new JKSTrustStore();
            trustStore.setContent(getContentAsBase64("truststore.jks"));

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(trustStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_throw_illegal_argument_exception_with_jks_key_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(new JKSKeyStore());

            final VertxWebSocketClientFactory factory = builder().sslOptions(sslOptions).build();
            assertThrows(IllegalArgumentException.class, factory::createWebSocketClient);
        }

        @Test
        void should_build_websocket_client_with_jks_key_store_path() {
            final JKSKeyStore keyStore = new JKSKeyStore();
            keyStore.setPath(getSslFilePath("client.jks"));
            keyStore.setPassword(PASSWORD);

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(keyStore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }

        @Test
        void should_build_websocket_client_with_jks_keystore_content() throws Exception {
            final JKSKeyStore keystore = new JKSKeyStore();
            keystore.setContent(getContentAsBase64("client.jks"));
            keystore.setPassword(PASSWORD);

            final SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(keystore);

            final WebSocketClient webSocketClient = builder().sslOptions(sslOptions).build().createWebSocketClient();

            assertThat(webSocketClient).isNotNull();
        }
    }

    private static WebSocketClientOptions extractWebSocketClientOptions(WebSocketClient webSocketClient) throws IllegalAccessException {
        Field webSocketClientImplField = ReflectionUtils.findField(
            CleanableWebSocketClient.class,
            "delegate",
            io.vertx.core.http.WebSocketClient.class
        );
        webSocketClientImplField.setAccessible(true);
        WebSocketClientImpl webSocketClientImpl = (WebSocketClientImpl) webSocketClientImplField.get(webSocketClient.getDelegate());

        Field websocketClientOptions = ReflectionUtils.findField(WebSocketClientImpl.class, "options", WebSocketClientOptions.class);
        websocketClientOptions.setAccessible(true);
        return (WebSocketClientOptions) websocketClientOptions.get(webSocketClientImpl);
    }

    private static String getSslFilePath(String file) {
        return Objects
            .requireNonNull(VertxWebSocketClientFactoryTest.class.getResource("/ssl/" + file), "File /ssl/" + file + " not found")
            .getPath();
    }

    private static String getSslFileContent(String file) throws IOException {
        return new String(
            Objects
                .requireNonNull(
                    VertxWebSocketClientFactoryTest.class.getResourceAsStream("/ssl/" + file),
                    "File /ssl/" + file + " not found"
                )
                .readAllBytes()
        );
    }

    private static String getContentAsBase64(String file) throws IOException {
        return new String(
            Base64
                .getEncoder()
                .encode(
                    Objects
                        .requireNonNull(
                            VertxWebSocketClientFactoryTest.class.getResourceAsStream("/ssl/" + file),
                            "File /ssl/" + file + " not found"
                        )
                        .readAllBytes()
                )
        );
    }
}
