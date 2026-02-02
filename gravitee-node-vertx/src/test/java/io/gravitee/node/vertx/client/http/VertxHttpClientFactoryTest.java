package io.gravitee.node.vertx.client.http;

import static io.gravitee.node.vertx.client.http.VertxHttpClientFactory.HTTP_SSL_OPENSSL_CONFIGURATION;
import static io.gravitee.node.vertx.client.http.VertxHttpProtocolVersion.HTTP_2;
import static io.gravitee.node.vertx.client.http.VertxHttpProxyType.HTTP;
import static io.vertx.core.http.Http2Settings.*;
import static io.vertx.core.http.HttpClientOptions.*;
import static io.vertx.core.http.HttpServerOptions.DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE;
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
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.impl.HttpClientImpl;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
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
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxHttpClientFactoryTest {

    protected static final String PASSWORD = "gravitee";
    private final Vertx vertx = Vertx.vertx();

    @Mock
    private Configuration nodeConfiguration;

    private VertxHttpClientFactory.VertxHttpClientFactoryBuilder builder() {
        final VertxHttpClientOptions httpOptions = VertxHttpClientOptions
            .builder()
            .keepAlive(true)
            .readTimeout(10000)
            .idleTimeout(60000)
            .keepAliveTimeout(30000)
            .connectTimeout(5000)
            .useCompression(false)
            .maxConcurrentConnections(100)
            .version(HTTP_2)
            .pipelining(false)
            .clearTextUpgrade(true)
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

        return VertxHttpClientFactory
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
    void should_build_client_with_default_http2_settings() {
        VertxHttpClientFactory.VertxHttpClientFactoryBuilder builder = builder();

        builder.httpOptions(VertxHttpClientOptions.builder().version(HTTP_2).build());
        final HttpClient httpClient = builder.build().createHttpClient();

        assertThat(httpClient).isNotNull();

        final HttpClientOptions httpClientOptions = extractHttpClientOptions(httpClient);
        assertThat(httpClientOptions).isNotNull();
        assertThat(httpClientOptions.getHttp2MultiplexingLimit()).isEqualTo(DEFAULT_HTTP2_MULTIPLEXING_LIMIT);
        assertThat(httpClientOptions.getHttp2ConnectionWindowSize()).isEqualTo(DEFAULT_HTTP2_CONNECTION_WINDOW_SIZE);
        assertThat(httpClientOptions.getInitialSettings().getInitialWindowSize()).isEqualTo(DEFAULT_INITIAL_WINDOW_SIZE);
        assertThat(httpClientOptions.getInitialSettings().getMaxConcurrentStreams()).isEqualTo(DEFAULT_MAX_CONCURRENT_STREAMS);
        assertThat(httpClientOptions.getInitialSettings().getMaxFrameSize()).isEqualTo(DEFAULT_MAX_FRAME_SIZE);
    }

    @Test
    @SneakyThrows
    void should_build_client_with_custom_http2_settings() {
        VertxHttpClientFactory.VertxHttpClientFactoryBuilder builder = builder();

        builder.httpOptions(
            VertxHttpClientOptions
                .builder()
                .version(HTTP_2)
                .http2MultiplexingLimit(13)
                .http2ConnectionWindowSize(128000)
                .http2StreamWindowSize(72000)
                .http2MaxFrameSize(32000)
                .build()
        );
        final HttpClient httpClient = builder.build().createHttpClient();

        assertThat(httpClient).isNotNull();

        final HttpClientOptions httpClientOptions = extractHttpClientOptions(httpClient);
        assertThat(httpClientOptions).isNotNull();
        assertThat(httpClientOptions.getHttp2MultiplexingLimit()).isEqualTo(13);
        assertThat(httpClientOptions.getHttp2ConnectionWindowSize()).isEqualTo(128000);
        assertThat(httpClientOptions.getInitialSettings().getInitialWindowSize()).isEqualTo(72000);
        assertThat(httpClientOptions.getInitialSettings().getMaxConcurrentStreams()).isEqualTo(13);
        assertThat(httpClientOptions.getInitialSettings().getMaxFrameSize()).isEqualTo(32000);
    }

    @Test
    @SneakyThrows
    void should_build_client_with_default_websocket_settings() {
        VertxHttpClientFactory.VertxHttpClientFactoryBuilder builder = builder();

        final HttpClient httpClient = builder.build().createHttpClient();

        assertThat(httpClient).isNotNull();

        final HttpClientOptions httpClientOptions = extractHttpClientOptions(httpClient);
        assertThat(httpClientOptions).isNotNull();
        assertThat(httpClientOptions.getMaxWebSocketFrameSize()).isEqualTo(DEFAULT_MAX_WEBSOCKET_FRAME_SIZE);
        assertThat(httpClientOptions.getMaxWebSocketMessageSize()).isEqualTo(DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE);
    }

    @Test
    @SneakyThrows
    void should_build_client_with_custom_websocket_settings() {
        VertxHttpClientFactory.VertxHttpClientFactoryBuilder builder = builder()
            .httpOptions(
                VertxHttpClientOptions.builder().version(HTTP_2).maxWebSocketFrameSize(1345).maxWebSocketMessageSize(1345 * 2).build()
            );
        final HttpClient httpClient = builder.build().createHttpClient();

        assertThat(httpClient).isNotNull();

        final HttpClientOptions httpClientOptions = extractHttpClientOptions(httpClient);
        assertThat(httpClientOptions).isNotNull();
        assertThat(httpClientOptions.getMaxWebSocketFrameSize()).isEqualTo(1345);
        assertThat(httpClientOptions.getMaxWebSocketMessageSize()).isEqualTo(1345 * 2);
    }

    private static HttpClientOptions extractHttpClientOptions(HttpClient httpClient) throws IllegalAccessException {
        Field httpOptionsFields = ReflectionUtils.findField(HttpClientImpl.class, "options", HttpClientOptions.class);
        httpOptionsFields.setAccessible(true);
        return (HttpClientOptions) httpOptionsFields.get(httpClient.getDelegate());
    }

    @Test
    void should_build_client_with_system_proxy() {
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
        final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().proxyOptions(proxyOptions);
        final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Nested
    class PEM {

        @Test
        void should_throw_illegal_argument_exception_with_PEM_trustStore_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            final PEMTrustStore trustStore = new PEMTrustStore();
            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder vertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            VertxHttpClientFactory factory = vertxHttpClientFactoryBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> factory.createHttpClient());
        }

        @Test
        void should_build_client_with_PEM_trustStore_path() {
            final String location = getSslFilePath("truststore.pem");

            final SslOptions sslOptions = new SslOptions();
            final PEMTrustStore trustStore = new PEMTrustStore();
            trustStore.setPath(location);

            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_build_client_with_PEM_trustStore_content() throws Exception {
            final String content = getSslFileContent("truststore.pem");

            final SslOptions sslOptions = new SslOptions();
            final PEMTrustStore trustStore = new PEMTrustStore();
            trustStore.setContent(content);

            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_throw_illegal_argument_exception_with_PEM_cert_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            final PEMKeyStore keystore = new PEMKeyStore();
            sslOptions.setKeyStore(keystore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder vertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            VertxHttpClientFactory factory = vertxHttpClientFactoryBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> factory.createHttpClient());
        }

        @Test
        void should_throw_illegal_argument_exception_with_pem_key_missing_path_or_content() {
            final String certLocation = getSslFilePath("client.cer");

            final SslOptions sslOptions = new SslOptions();
            final PEMKeyStore keyStore = new PEMKeyStore();
            sslOptions.setKeyStore(keyStore);
            keyStore.setCertPath(certLocation);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder vertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            VertxHttpClientFactory factory = vertxHttpClientFactoryBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> factory.createHttpClient());
        }

        @Test
        void should_build_client_with_pem_key_store_path() {
            final String keyLocation = getSslFilePath("client.key");
            final String certLocation = getSslFilePath("client.cer");

            final SslOptions sslOptions = new SslOptions();
            final PEMKeyStore keyStore = new PEMKeyStore();
            keyStore.setKeyPath(keyLocation);
            keyStore.setCertPath(certLocation);

            sslOptions.setKeyStore(keyStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_build_client_with_pem_keystore_content() throws Exception {
            final String keyContent = getSslFileContent("client.key");
            final String certContent = getSslFileContent("client.cer");

            final SslOptions sslOptions = new SslOptions();
            final PEMKeyStore keystore = new PEMKeyStore();
            keystore.setKeyContent(keyContent);
            keystore.setCertContent(certContent);

            sslOptions.setKeyStore(keystore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }
    }

    @Nested
    class PKCS12 {

        @Test
        void should_throw_illegal_argument_exception_with_pkcs12_trust_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            final PKCS12TrustStore trustStore = new PKCS12TrustStore();
            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder vertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            VertxHttpClientFactory factory = vertxHttpClientFactoryBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> factory.createHttpClient());
        }

        @Test
        void should_build_client_with_pkcs12_trust_store_path() {
            final String location = getSslFilePath("truststore.p12");

            final SslOptions sslOptions = new SslOptions();
            final PKCS12TrustStore trustStore = new PKCS12TrustStore();
            trustStore.setPath(location);

            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_build_client_with_pkcs12_trust_store_content() throws Exception {
            final String content = getContentAsBase64("truststore.p12");

            final SslOptions sslOptions = new SslOptions();
            final PKCS12TrustStore trustStore = new PKCS12TrustStore();
            trustStore.setContent(content);

            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_throw_illegal_argument_exception_with_pkcs12_key_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            final PKCS12KeyStore keyStore = new PKCS12KeyStore();
            sslOptions.setKeyStore(keyStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder vertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            VertxHttpClientFactory factory = vertxHttpClientFactoryBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> factory.createHttpClient());
        }

        @Test
        void should_build_client_with_pkcs12_key_store_path() {
            final String location = getSslFilePath("client.p12");

            final SslOptions sslOptions = new SslOptions();
            final PKCS12KeyStore keyStore = new PKCS12KeyStore();
            keyStore.setPath(location);
            keyStore.setPassword(PASSWORD);

            sslOptions.setKeyStore(keyStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_build_client_with_pkcs12_keystore_content() throws Exception {
            final String content = getContentAsBase64("client.p12");

            final SslOptions sslOptions = new SslOptions();
            final PKCS12KeyStore keystore = new PKCS12KeyStore();
            keystore.setContent(content);
            keystore.setPassword(PASSWORD);

            sslOptions.setKeyStore(keystore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }
    }

    @Nested
    class JKS {

        @Test
        void should_throw_illegal_argument_exception_with_jks_trust_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            final JKSTrustStore trustStore = new JKSTrustStore();
            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            VertxHttpClientFactory factory = VertxHttpClientFactoryBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> factory.createHttpClient());
        }

        @Test
        void should_build_client_with_jks_trust_store_path() {
            final String location = getSslFilePath("truststore.jks");

            final SslOptions sslOptions = new SslOptions();
            final JKSTrustStore trustStore = new JKSTrustStore();
            trustStore.setPath(location);

            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_build_client_with_jks_trust_store_content() throws Exception {
            final String content = getContentAsBase64("truststore.jks");

            final SslOptions sslOptions = new SslOptions();
            final JKSTrustStore trustStore = new JKSTrustStore();
            trustStore.setContent(content);

            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_throw_illegal_argument_exception_with_jks_key_store_missing_path_or_content() {
            final SslOptions sslOptions = new SslOptions();
            final JKSKeyStore keyStore = new JKSKeyStore();
            sslOptions.setKeyStore(keyStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder vertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            VertxHttpClientFactory factory = vertxHttpClientFactoryBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> factory.createHttpClient());
        }

        @Test
        void should_build_client_with_jks_key_store_path() {
            final String location = getSslFilePath("client.jks");

            final SslOptions sslOptions = new SslOptions();
            final JKSKeyStore keyStore = new JKSKeyStore();
            keyStore.setPath(location);
            keyStore.setPassword(PASSWORD);

            sslOptions.setKeyStore(keyStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }

        @Test
        void should_build_client_with_jks_keystore_content() throws Exception {
            final String content = getContentAsBase64("client.jks");

            final SslOptions sslOptions = new SslOptions();
            final JKSKeyStore keystore = new JKSKeyStore();
            keystore.setContent(content);
            keystore.setPassword(PASSWORD);

            sslOptions.setKeyStore(keystore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);
            final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

            assertThat(httpClient).isNotNull();
        }
    }

    private static String getSslFilePath(String file) {
        return Objects
            .requireNonNull(VertxHttpClientFactoryTest.class.getResource("/ssl/" + file), "File /ssl/" + file + " not found")
            .getPath();
    }

    private static String getSslFileContent(String file) throws IOException {
        return new String(
            Objects
                .requireNonNull(VertxHttpClientFactoryTest.class.getResourceAsStream("/ssl/" + file), "File /ssl/" + file + " not found")
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
                            VertxHttpClientFactoryTest.class.getResourceAsStream("/ssl/" + file),
                            "File /ssl/" + file + " not found"
                        )
                        .readAllBytes()
                )
        );
    }
}
