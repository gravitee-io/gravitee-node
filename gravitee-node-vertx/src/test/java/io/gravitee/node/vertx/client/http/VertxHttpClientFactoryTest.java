package io.gravitee.node.vertx.client.http;

import static io.gravitee.node.vertx.client.http.VertxHttpClientFactory.HTTP_SSL_OPENSSL_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.ssl.jks.JKSKeyStore;
import io.gravitee.node.vertx.client.ssl.jks.JKSTrustStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMKeyStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMTrustStore;
import io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12TrustStore;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxHttpClientFactoryTest {

    protected static final String PASSWORD = "gravitee";
    private final Vertx vertx = Vertx.vertx();
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private Configuration nodeConfiguration;

    private VertxHttpClientFactory.VertxHttpClientFactoryBuilder builder() throws Exception {
        final VertxHttpClientOptions httpOptions = mapper.readValue(HTTP_CONFIG, VertxHttpClientOptions.class);
        final VertxHttpProxyOptions proxyOptions = mapper.readValue(PROXY_CONFIG, VertxHttpProxyOptions.class);

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
    void should_build_client_with_system_proxy() throws Exception {
        final VertxHttpProxyOptions proxyOptions = mapper.readValue(PROXY_CONFIG, VertxHttpProxyOptions.class);
        proxyOptions.setUseSystemProxy(true);

        final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().proxyOptions(proxyOptions);
        final HttpClient httpClient = VertxHttpClientFactoryBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Nested
    class PEM {

        @Test
        void should_throw_illegal_argument_exception_with_PEM_trustStore_missing_path_or_content() throws Exception {
            final SslOptions sslOptions = new SslOptions();
            final PEMTrustStore trustStore = new PEMTrustStore();
            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            assertThrows(IllegalArgumentException.class, () -> VertxHttpClientFactoryBuilder.build().createHttpClient());
        }

        @Test
        void should_build_client_with_PEM_trustStore_path() throws Exception {
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
        void should_throw_illegal_argument_exception_with_PEM_cert_missing_path_or_content() throws Exception {
            final SslOptions sslOptions = new SslOptions();
            final PEMKeyStore keystore = new PEMKeyStore();
            sslOptions.setKeyStore(keystore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            assertThrows(IllegalArgumentException.class, () -> VertxHttpClientFactoryBuilder.build().createHttpClient());
        }

        @Test
        void should_throw_illegal_argument_exception_with_pem_key_missing_path_or_content() throws Exception {
            final String certLocation = getSslFilePath("client.cer");

            final SslOptions sslOptions = new SslOptions();
            final PEMKeyStore keyStore = new PEMKeyStore();
            sslOptions.setKeyStore(keyStore);
            keyStore.setCertPath(certLocation);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            assertThrows(IllegalArgumentException.class, () -> VertxHttpClientFactoryBuilder.build().createHttpClient());
        }

        @Test
        void should_build_client_with_pem_key_store_path() throws Exception {
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
        void should_throw_illegal_argument_exception_with_pkcs12_trust_store_missing_path_or_content() throws Exception {
            final SslOptions sslOptions = new SslOptions();
            final PKCS12TrustStore trustStore = new PKCS12TrustStore();
            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            assertThrows(IllegalArgumentException.class, () -> VertxHttpClientFactoryBuilder.build().createHttpClient());
        }

        @Test
        void should_build_client_with_pkcs12_trust_store_path() throws Exception {
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
        void should_throw_illegal_argument_exception_with_pkcs12_key_store_missing_path_or_content() throws Exception {
            final SslOptions sslOptions = new SslOptions();
            final PKCS12KeyStore keyStore = new PKCS12KeyStore();
            sslOptions.setKeyStore(keyStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            assertThrows(IllegalArgumentException.class, () -> VertxHttpClientFactoryBuilder.build().createHttpClient());
        }

        @Test
        void should_build_client_with_pkcs12_key_store_path() throws Exception {
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
        void should_throw_illegal_argument_exception_with_jks_trust_store_missing_path_or_content() throws Exception {
            final SslOptions sslOptions = new SslOptions();
            final JKSTrustStore trustStore = new JKSTrustStore();
            sslOptions.setTrustStore(trustStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            assertThrows(IllegalArgumentException.class, () -> VertxHttpClientFactoryBuilder.build().createHttpClient());
        }

        @Test
        void should_build_client_with_jks_trust_store_path() throws Exception {
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
        void should_throw_illegal_argument_exception_with_jks_key_store_missing_path_or_content() throws Exception {
            final SslOptions sslOptions = new SslOptions();
            final JKSKeyStore keyStore = new JKSKeyStore();
            sslOptions.setKeyStore(keyStore);

            final VertxHttpClientFactory.VertxHttpClientFactoryBuilder VertxHttpClientFactoryBuilder = builder().sslOptions(sslOptions);

            assertThrows(IllegalArgumentException.class, () -> VertxHttpClientFactoryBuilder.build().createHttpClient());
        }

        @Test
        void should_build_client_with_jks_key_store_path() throws Exception {
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

    private static final String HTTP_CONFIG =
        """
                  {
                      "keepAlive": true,
                      "followRedirects": false,
                      "readTimeout": 10000,
                      "idleTimeout": 60000,
                      "connectTimeout": 5000,
                      "propagateClientAcceptEncoding": true,
                      "useCompression": false,
                      "maxConcurrentConnections": 100,
                      "version": "HTTP_2",
                      "pipelining": false,
                      "clearTextUpgrade": true
                  }""";

    private static final String PROXY_CONFIG =
        """
                  {
                      "enabled": true,
                      "useSystemProxy": false,
                      "host": "localhost",
                      "port": 8080,
                      "username": "user",
                      "password": "pwd",
                      "type": "HTTP"
                  }""";
}
