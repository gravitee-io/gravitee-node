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

import static io.gravitee.node.vertx.server.http.VertxHttpServerOptions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.tracing.TracingPolicy;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxHttpServerOptionsTest {

    public static final String SERVER_ID = "test";
    public static final String MAX_INITIAL_LINE_LENGTH = "1234";
    public static final String MAX_CHUNK_SIZE = "4321";
    public static final String MAX_HEADER_SIZE = "5678";
    public static final String MAX_FORM_ATTRIBUTE_SIZE = "7612";
    public static final String COMPRESSION_SUPPORTED = "true";
    public static final String TCP_KEEP_ALIVE = "true";
    public static final String TRACING_POLICY = "ignore";
    public static final String HANDLE_100_CONTINUE = "true";
    public static final String SECURED = "true";
    public static final String ALPN = "true";
    public static final String IDLE_TIMEOUT = "5000";
    public static final String HOST = "10.1.1.1";
    public static final String PORT = "7890";
    public static final String REQUEST_TIMEOUT = "32000";
    public static final String CLIENT_AUTH = "request";
    public static final String TLS_PROTOCOLS = "TLSv1.3";
    public static final String TLS_CIPHERS = "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA";
    public static final String KEYSTORE_TYPE = "pkcs12";
    public static final String TRUSTSTORE_TYPE = "jks";
    public static final String KEYSTORE_PATH = "/server.p12";
    public static final String KEYSTORE_PASSWORD = "s3cr3t-k3ystore";
    public static final String TRUSTSTORE_PATH = "/ca.jks";
    public static final String TRUSTSTORE_PASSWORD = "s3cr3t-truststore";
    public static final String SNI = "true";
    public static final String OPENSSL = "true";
    public static final String WEBSOCKET_ENABLED = "true";
    public static final String WEBSOCKET_SUB_PROTOCOLS = "v10.stomp, v11.stomp, v12.stomp";
    public static final String WEBSOCKET_PER_MESSAGE_COMPRESSION_SUPPORTED = "true";
    public static final String WEBSOCKET_PER_FRAME_COMPRESSION_SUPPORTED = "true";
    public static final String MAX_WEBSOCKET_MESSAGE_SIZE = "90900";
    public static final String MAX_WEBSOCKET_FRAME_SIZE = "88888";
    public static final String HAPROXY_PROTOCOL = "true";
    public static final String HAPROXY_PROTOCOL_TIMEOUT = "12000";

    @Mock
    private KeyStoreLoaderManager keyStoreLoaderManager;

    private MockEnvironment environment;

    @BeforeEach
    void init() {
        environment = new MockEnvironment();
        environment
            .withProperty("servers[0].id", SERVER_ID)
            .withProperty("servers[0].maxInitialLineLength", MAX_INITIAL_LINE_LENGTH)
            .withProperty("servers[0].maxChunkSize", MAX_CHUNK_SIZE)
            .withProperty("servers[0].maxHeaderSize", MAX_HEADER_SIZE)
            .withProperty("servers[0].maxFormAttributeSize", MAX_FORM_ATTRIBUTE_SIZE)
            .withProperty("servers[0].compressionSupported", COMPRESSION_SUPPORTED)
            .withProperty("servers[0].tcpKeepAlive", TCP_KEEP_ALIVE)
            .withProperty("servers[0].idleTimeout", IDLE_TIMEOUT)
            .withProperty("servers[0].tracingPolicy", TRACING_POLICY)
            .withProperty("servers[0].handle100Continue", HANDLE_100_CONTINUE)
            .withProperty("servers[0].host", HOST)
            .withProperty("servers[0].port", PORT)
            .withProperty("servers[0].secured", SECURED)
            .withProperty("servers[0].alpn", ALPN)
            .withProperty("servers[0].ssl.clientAuth", CLIENT_AUTH)
            .withProperty("servers[0].ssl.tlsProtocols", TLS_PROTOCOLS)
            .withProperty("servers[0].ssl.tlsCiphers", TLS_CIPHERS)
            .withProperty("servers[0].ssl.keystore.type", KEYSTORE_TYPE)
            .withProperty("servers[0].ssl.keystore.path", KEYSTORE_PATH)
            .withProperty("servers[0].ssl.keystore.password", KEYSTORE_PASSWORD)
            .withProperty("servers[0].ssl.truststore.type", TRUSTSTORE_TYPE)
            .withProperty("servers[0].ssl.truststore.path", TRUSTSTORE_PATH)
            .withProperty("servers[0].ssl.truststore.password", TRUSTSTORE_PASSWORD)
            .withProperty("servers[0].ssl.sni", SNI)
            .withProperty("servers[0].ssl.openssl", OPENSSL)
            .withProperty("servers[0].websocket.enabled", WEBSOCKET_ENABLED)
            .withProperty("servers[0].websocket.subProtocols", WEBSOCKET_SUB_PROTOCOLS)
            .withProperty("servers[0].websocket.perMessageWebSocketCompressionSupported", WEBSOCKET_PER_MESSAGE_COMPRESSION_SUPPORTED)
            .withProperty("servers[0].websocket.maxWebSocketMessageSize", MAX_WEBSOCKET_MESSAGE_SIZE)
            .withProperty("servers[0].websocket.maxWebSocketFrameSize", MAX_WEBSOCKET_FRAME_SIZE)
            .withProperty("servers[0].haproxy.proxyProtocol", HAPROXY_PROTOCOL)
            .withProperty("servers[0].haproxy.proxyProtocolTimeout", HAPROXY_PROTOCOL_TIMEOUT);
    }

    @Test
    void should_build_from_environment_configuration() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getId()).isEqualTo(SERVER_ID);
        assertThat(options.getMaxInitialLineLength()).isEqualTo(Integer.valueOf(MAX_INITIAL_LINE_LENGTH));
        assertThat(options.getMaxChunkSize()).isEqualTo(Integer.valueOf(MAX_CHUNK_SIZE));
        assertThat(options.getMaxHeaderSize()).isEqualTo(Integer.valueOf(MAX_HEADER_SIZE));
        assertThat(options.getMaxFormAttributeSize()).isEqualTo(Integer.valueOf(MAX_FORM_ATTRIBUTE_SIZE));
        assertThat(options.isCompressionSupported()).isEqualTo(Boolean.valueOf(COMPRESSION_SUPPORTED));
        assertThat(options.isTcpKeepAlive()).isEqualTo(Boolean.valueOf(TCP_KEEP_ALIVE));
        assertThat(options.getIdleTimeout()).isEqualTo(Integer.valueOf(IDLE_TIMEOUT));
        assertThat(options.getTracingPolicy()).isEqualTo(TRACING_POLICY);
        assertThat(options.isHandle100Continue()).isEqualTo(Boolean.valueOf(HANDLE_100_CONTINUE));
        assertThat(options.getHost()).isEqualTo(HOST);
        assertThat(options.getPort()).isEqualTo(Integer.valueOf(PORT));
        assertThat(options.isSecured()).isEqualTo(Boolean.valueOf(SECURED));
        assertThat(options.isAlpn()).isEqualTo(Boolean.valueOf(ALPN));
        assertThat(options.getClientAuth()).isEqualToIgnoringCase(CLIENT_AUTH);
        assertThat(options.getTlsProtocols()).isEqualTo(TLS_PROTOCOLS);
        assertThat(options.getAuthorizedTlsCipherSuites()).isEqualTo(Arrays.asList(TLS_CIPHERS.split(",\\s?")));
        assertThat(options.getKeyStoreType()).isEqualToIgnoringCase(KEYSTORE_TYPE);
        assertThat(options.getKeyStorePath()).isEqualTo(KEYSTORE_PATH);
        assertThat(options.getKeyStorePassword()).isEqualTo(KEYSTORE_PASSWORD);
        assertThat(options.getTrustStoreType()).isEqualToIgnoringCase(TRUSTSTORE_TYPE);
        assertThat(options.getTrustStorePath()).isEqualTo(TRUSTSTORE_PATH);
        assertThat(options.getTrustStorePassword()).isEqualTo(TRUSTSTORE_PASSWORD);
        assertThat(options.isSni()).isEqualTo(Boolean.valueOf(SNI));
        assertThat(options.isOpenssl()).isEqualTo(Boolean.valueOf(OPENSSL));
        assertThat(options.isWebsocketEnabled()).isEqualTo(Boolean.valueOf(WEBSOCKET_ENABLED));
        assertThat(options.getWebsocketSubProtocols()).isEqualTo(WEBSOCKET_SUB_PROTOCOLS);
        assertThat(options.isPerMessageWebSocketCompressionSupported())
            .isEqualTo(Boolean.valueOf(WEBSOCKET_PER_MESSAGE_COMPRESSION_SUPPORTED));
        assertThat(options.isPerFrameWebSocketCompressionSupported()).isEqualTo(Boolean.valueOf(WEBSOCKET_PER_FRAME_COMPRESSION_SUPPORTED));
        assertThat(options.getMaxWebSocketMessageSize()).isEqualTo(Integer.valueOf(MAX_WEBSOCKET_MESSAGE_SIZE));
        assertThat(options.getMaxWebSocketFrameSize()).isEqualTo(Integer.valueOf(MAX_WEBSOCKET_FRAME_SIZE));
        assertThat(options.isHaProxyProtocol()).isEqualTo(Boolean.valueOf(HAPROXY_PROTOCOL));
        assertThat(options.getHaProxyProtocolTimeout()).isEqualTo(Long.valueOf(HAPROXY_PROTOCOL_TIMEOUT));
    }

    @Test
    void should_build_from_environment_configuration_with_default_values_when_unknown_prefix() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().prefix("unknown").environment(environment).build();

        assertThat(options.getId()).isNotEqualTo(SERVER_ID); // Server id is generated when not provided.
        assertThat(options.getMaxInitialLineLength()).isEqualTo(DEFAULT_MAX_INITIAL_LINE_LENGTH);
        assertThat(options.getMaxChunkSize()).isEqualTo(DEFAULT_MAX_CHUNK_SIZE);
        assertThat(options.getMaxHeaderSize()).isEqualTo(DEFAULT_MAX_HEADER_SIZE);
        assertThat(options.getMaxFormAttributeSize()).isEqualTo(DEFAULT_MAX_FORM_ATTRIBUTE_SIZE);
        assertThat(options.isCompressionSupported()).isEqualTo(DEFAULT_COMPRESSION_SUPPORTED);
        assertThat(options.isTcpKeepAlive()).isEqualTo(DEFAULT_TCP_KEEP_ALIVE);
        assertThat(options.getIdleTimeout()).isEqualTo(DEFAULT_IDLE_TIMEOUT);
        assertThat(options.getTracingPolicy()).isEqualTo(DEFAULT_TRACING_POLICY);
        assertThat(options.isHandle100Continue()).isEqualTo(DEFAULT_HANDLE_100_CONTINUE);
        assertThat(options.getHost()).isEqualTo(DEFAULT_LISTENING_HOST);
        assertThat(options.getPort()).isEqualTo(DEFAULT_PORT);
        assertThat(options.isSecured()).isEqualTo(DEFAULT_SECURED);
        assertThat(options.isAlpn()).isEqualTo(DEFAULT_ALPN);
        assertThat(options.getClientAuth()).isEqualToIgnoringCase(DEFAULT_CLIENT_AUTH);
        assertThat(options.getTlsProtocols()).isNull();
        assertThat(options.getAuthorizedTlsCipherSuites()).isNull();
        assertThat(options.getKeyStoreType()).isEqualToIgnoringCase(DEFAULT_STORE_TYPE);
        assertThat(options.getKeyStorePath()).isNull();
        assertThat(options.getKeyStorePassword()).isNull();
        assertThat(options.getTrustStoreType()).isEqualToIgnoringCase(DEFAULT_STORE_TYPE);
        assertThat(options.getTrustStorePath()).isNull();
        assertThat(options.getTrustStorePassword()).isNull();
        assertThat(options.isSni()).isEqualTo(DEFAULT_SNI);
        assertThat(options.isOpenssl()).isEqualTo(DEFAULT_OPENSSL);
        assertThat(options.isWebsocketEnabled()).isEqualTo(DEFAULT_WEBSOCKET_ENABLED);
        assertThat(options.getWebsocketSubProtocols()).isNull();
        assertThat(options.isPerMessageWebSocketCompressionSupported()).isEqualTo(DEFAULT_PER_MESSAGE_WEBSOCKET_COMPRESSION_SUPPORTED);
        assertThat(options.isPerFrameWebSocketCompressionSupported()).isEqualTo(DEFAULT_PER_FRAME_WEBSOCKET_COMPRESSION_SUPPORTED);
        assertThat(options.getMaxWebSocketMessageSize()).isEqualTo(DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE);
        assertThat(options.getMaxWebSocketFrameSize()).isEqualTo(DEFAULT_MAX_WEBSOCKET_FRAME_SIZE);
        assertThat(options.isHaProxyProtocol()).isEqualTo(DEFAULT_HAPROXY_PROTOCOL);
        assertThat(options.getHaProxyProtocolTimeout()).isEqualTo(Long.valueOf(DEFAULT_HAPROXY_PROTOCOL_TIMEOUT));
    }

    @Test
    void should_build_from_environment_configuration_with_pem_keystore() {
        environment.setProperty("servers[0].ssl.keystore.type", "pem");
        environment.setProperty("servers[0].ssl.keystore.certificates[0].cert", "cert1.pem");
        environment.setProperty("servers[0].ssl.keystore.certificates[0].key", "key1.pem");
        environment.setProperty("servers[0].ssl.keystore.certificates[1].cert", "cert2.pem");
        environment.setProperty("servers[0].ssl.keystore.certificates[1].key", "key2.pem");

        final CertificateOptions certOptions1 = CertificateOptions.builder().certificate("cert1.pem").privateKey("key1.pem").build();
        final CertificateOptions certOptions2 = CertificateOptions.builder().certificate("cert2.pem").privateKey("key2.pem").build();

        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getKeyStoreType()).isEqualToIgnoringCase("pem");
        assertThat(options.getKeyStoreCertificates()).isEqualTo(List.of(certOptions1, certOptions2));
        assertThat(options.getKeyStorePassword()).isEqualTo(KEYSTORE_PASSWORD);
    }

    @Test
    void should_build_from_environment_configuration_with_pem_truststore_and_multiple_paths() {
        environment.setProperty("servers[0].ssl.truststore.type", "pem");
        environment
            .getPropertySources()
            .stream()
            .findFirst()
            .ifPresent(propertySource -> ((PropertySource<Properties>) propertySource).getSource().remove("servers[0].ssl.truststore.path")
            );
        environment.setProperty("servers[0].ssl.truststore.path[0]", "cert1.pem");
        environment.setProperty("servers[0].ssl.truststore.path[1]", "cert2.pem");

        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getTrustStoreType()).isEqualToIgnoringCase("pem");
        assertThat(options.getTrustStorePath()).isNull();
        assertThat(options.getTrustStorePaths()).isEqualTo(List.of("cert1.pem", "cert2.pem"));
        assertThat(options.getTrustStorePassword()).isEqualTo(TRUSTSTORE_PASSWORD);
    }

    @Test
    void should_build_from_environment_configuration_with_client_auth_true() {
        // Support for old version where clientAuth property supported boolean.
        environment.setProperty("servers[0].ssl.clientAuth", "true");

        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getClientAuth()).isEqualToIgnoringCase(ClientAuth.REQUIRED.name());
    }

    @Test
    void should_build_from_environment_configuration_with_client_auth_false() {
        // Support for old version where clientAuth property supported boolean.
        environment.setProperty("servers[0].ssl.clientAuth", "false");

        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getClientAuth()).isEqualToIgnoringCase(ClientAuth.NONE.name());
    }

    @Test
    void should_throw_illegal_argument_exception_from_environment_configuration_when_prefix_is_not_set() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> builder().environment(environment).build()
        );

        assertThat(exception.getMessage()).isEqualTo("Prefix must be set before environment");
    }

    @Test
    void should_build_with_default() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().build();

        assertThat(options.getId()).isNotEqualTo(SERVER_ID); // Server id is generated when not provided.
        assertThat(options.getMaxInitialLineLength()).isEqualTo(DEFAULT_MAX_INITIAL_LINE_LENGTH);
        assertThat(options.getMaxChunkSize()).isEqualTo(DEFAULT_MAX_CHUNK_SIZE);
        assertThat(options.getMaxHeaderSize()).isEqualTo(DEFAULT_MAX_HEADER_SIZE);
        assertThat(options.isCompressionSupported()).isEqualTo(DEFAULT_COMPRESSION_SUPPORTED);
        assertThat(options.isTcpKeepAlive()).isEqualTo(DEFAULT_TCP_KEEP_ALIVE);
        assertThat(options.getIdleTimeout()).isEqualTo(DEFAULT_IDLE_TIMEOUT);
        assertThat(options.getTracingPolicy()).isEqualTo(DEFAULT_TRACING_POLICY);
        assertThat(options.isHandle100Continue()).isEqualTo(DEFAULT_HANDLE_100_CONTINUE);
        assertThat(options.getHost()).isEqualTo(DEFAULT_LISTENING_HOST);
        assertThat(options.getPort()).isEqualTo(DEFAULT_PORT);
        assertThat(options.isSecured()).isEqualTo(DEFAULT_SECURED);
        assertThat(options.isAlpn()).isEqualTo(DEFAULT_ALPN);
        assertThat(options.getClientAuth()).isEqualToIgnoringCase(DEFAULT_CLIENT_AUTH);
        assertThat(options.getTlsProtocols()).isNull();
        assertThat(options.getAuthorizedTlsCipherSuites()).isNull();
        assertThat(options.getKeyStoreType()).isEqualToIgnoringCase(DEFAULT_STORE_TYPE);
        assertThat(options.getKeyStorePath()).isNull();
        assertThat(options.getKeyStorePassword()).isNull();
        assertThat(options.getTrustStoreType()).isEqualToIgnoringCase(DEFAULT_STORE_TYPE);
        assertThat(options.getTrustStorePath()).isNull();
        assertThat(options.getTrustStorePassword()).isNull();
        assertThat(options.isSni()).isEqualTo(DEFAULT_SNI);
        assertThat(options.isOpenssl()).isEqualTo(DEFAULT_OPENSSL);
        assertThat(options.isWebsocketEnabled()).isEqualTo(DEFAULT_WEBSOCKET_ENABLED);
        assertThat(options.getWebsocketSubProtocols()).isNull();
        assertThat(options.isPerMessageWebSocketCompressionSupported()).isEqualTo(DEFAULT_PER_MESSAGE_WEBSOCKET_COMPRESSION_SUPPORTED);
        assertThat(options.isPerFrameWebSocketCompressionSupported()).isEqualTo(DEFAULT_PER_FRAME_WEBSOCKET_COMPRESSION_SUPPORTED);
        assertThat(options.getMaxWebSocketMessageSize()).isEqualTo(DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE);
        assertThat(options.getMaxWebSocketFrameSize()).isEqualTo(DEFAULT_MAX_WEBSOCKET_FRAME_SIZE);
        assertThat(options.isHaProxyProtocol()).isEqualTo(DEFAULT_HAPROXY_PROTOCOL);
        assertThat(options.getHaProxyProtocolTimeout()).isEqualTo(Long.valueOf(DEFAULT_HAPROXY_PROTOCOL_TIMEOUT));
    }

    @Test
    void should_build_with_default_and_use_specified_default_port_when_no_port_explicitly_set() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().defaultPort(1234).build();

        assertThat(options.getPort()).isEqualTo(1234);
    }

    @Test
    void should_build_with_default_and_not_use_specified_default_port_when_port_explicitly_set() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().port(5678).defaultPort(1234).build();

        assertThat(options.getPort()).isEqualTo(5678);
    }

    @Test
    void should_build_with_default_and_use_specified_default_port_when_no_port_explicitly_set_using_environment_configuration() {
        // Remove port from environment configuration.
        environment
            .getPropertySources()
            .stream()
            .findFirst()
            .ifPresent(propertySource -> ((PropertySource<Properties>) propertySource).getSource().remove("servers[0].port"));

        final VertxHttpServerOptions options = VertxHttpServerOptions
            .builder()
            .defaultPort(9876)
            .prefix("servers[0]")
            .environment(environment)
            .build();

        assertThat(options.getPort()).isEqualTo(9876);
    }

    @Test
    void should_build_with_default_and_not_use_specified_default_port_when_port_explicitly_set_using_environment_configuration() {
        final VertxHttpServerOptions options = VertxHttpServerOptions
            .builder()
            .defaultPort(1234)
            .prefix("servers[0]")
            .environment(environment)
            .build();

        assertThat(options.getPort()).isEqualTo(Integer.valueOf(PORT));
    }

    @Test
    void should_create_vertx_options() {
        final VertxHttpServerOptions options = VertxHttpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .build();

        when(keyStoreLoaderManager.create(any(KeyStoreLoaderOptions.class))).thenReturn(mock(KeyStoreLoader.class));

        final HttpServerOptions httpServerOptions = options.createHttpServerOptions();

        assertThat(httpServerOptions).isNotNull();
        assertThat(httpServerOptions.getMaxInitialLineLength()).isEqualTo(Integer.valueOf(MAX_INITIAL_LINE_LENGTH));
        assertThat(httpServerOptions.getMaxChunkSize()).isEqualTo(Integer.valueOf(MAX_CHUNK_SIZE));
        assertThat(httpServerOptions.getMaxHeaderSize()).isEqualTo(Integer.valueOf(MAX_HEADER_SIZE));
        assertThat(httpServerOptions.getMaxFormAttributeSize()).isEqualTo(Integer.valueOf(MAX_FORM_ATTRIBUTE_SIZE));
        assertThat(httpServerOptions.isCompressionSupported()).isEqualTo(Boolean.valueOf(COMPRESSION_SUPPORTED));
        assertThat(httpServerOptions.isTcpKeepAlive()).isEqualTo(Boolean.valueOf(TCP_KEEP_ALIVE));
        assertThat(httpServerOptions.getIdleTimeout()).isEqualTo(Integer.valueOf(IDLE_TIMEOUT));
        assertThat(httpServerOptions.getTracingPolicy()).isEqualTo(TracingPolicy.valueOf(TRACING_POLICY.toUpperCase()));
        assertThat(httpServerOptions.isHandle100ContinueAutomatically()).isEqualTo(Boolean.valueOf(HANDLE_100_CONTINUE));
        assertThat(httpServerOptions.getHost()).isEqualTo(HOST);
        assertThat(httpServerOptions.getPort()).isEqualTo(Integer.valueOf(PORT));
        assertThat(httpServerOptions.isUseAlpn()).isEqualTo(Boolean.valueOf(ALPN));
        assertThat(httpServerOptions.getClientAuth()).isEqualTo(ClientAuth.valueOf(CLIENT_AUTH.toUpperCase()));
        assertThat(httpServerOptions.getEnabledSecureTransportProtocols()).isEqualTo(Set.of(TLS_PROTOCOLS));
        assertThat(httpServerOptions.getEnabledCipherSuites()).isEqualTo(new HashSet<>(Arrays.asList(TLS_CIPHERS.split(",\\s?"))));
        assertThat(httpServerOptions.getKeyCertOptions()).isNotNull();
        assertThat(httpServerOptions.getTrustOptions()).isNotNull();
        assertThat(httpServerOptions.isSsl()).isEqualTo(Boolean.valueOf(SECURED));
        assertThat(httpServerOptions.isSni()).isEqualTo(Boolean.valueOf(SNI));
        assertThat(httpServerOptions.getOpenSslEngineOptions()).isNotNull();
        assertThat(httpServerOptions.getWebSocketSubProtocols()).containsAll(List.of(WEBSOCKET_SUB_PROTOCOLS.split(",\\s?")));
        assertThat(httpServerOptions.getPerMessageWebSocketCompressionSupported())
            .isEqualTo(Boolean.valueOf(WEBSOCKET_PER_MESSAGE_COMPRESSION_SUPPORTED));
        assertThat(httpServerOptions.getPerFrameWebSocketCompressionSupported())
            .isEqualTo(Boolean.valueOf(WEBSOCKET_PER_FRAME_COMPRESSION_SUPPORTED));
        assertThat(httpServerOptions.getMaxWebSocketMessageSize()).isEqualTo(Integer.valueOf(MAX_WEBSOCKET_MESSAGE_SIZE));
        assertThat(httpServerOptions.getMaxWebSocketFrameSize()).isEqualTo(Integer.valueOf(MAX_WEBSOCKET_FRAME_SIZE));
        assertThat(httpServerOptions.isUseProxyProtocol()).isEqualTo(Boolean.valueOf(HAPROXY_PROTOCOL));
        assertThat(httpServerOptions.getProxyProtocolTimeout()).isEqualTo(Long.valueOf(HAPROXY_PROTOCOL_TIMEOUT));
    }

    @Test
    void should_throw_illegal_argument_exception_when_create_vertx_options_with_secured_enabled_and_no_keystore_loader_manager() {
        final VertxHttpServerOptions options = VertxHttpServerOptions.builder().prefix("servers[0]").environment(environment).build();
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::createHttpServerOptions);

        assertThat(exception.getMessage()).isEqualTo("You must provide a KeyStoreLoaderManager when 'secured' is enabled.");
    }

    @Test
    void should_create_vertx_options_with_pem_truststore() {
        environment.setProperty("servers[0].ssl.truststore.type", "pem");
        environment
            .getPropertySources()
            .stream()
            .findFirst()
            .ifPresent(propertySource -> ((PropertySource<Properties>) propertySource).getSource().remove("servers[0].ssl.truststore.path")
            );
        environment.setProperty("servers[0].ssl.truststore.path[0]", "cert1.pem");
        environment.setProperty("servers[0].ssl.truststore.path[1]", "cert2.pem");

        when(keyStoreLoaderManager.create(any(KeyStoreLoaderOptions.class))).thenReturn(mock(KeyStoreLoader.class));

        final VertxHttpServerOptions options = VertxHttpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .build();
        final HttpServerOptions httpServerOptions = options.createHttpServerOptions();

        assertThat(httpServerOptions).isNotNull();
        assertThat(httpServerOptions.getPemTrustOptions()).isNotNull();
        assertThat(httpServerOptions.getPfxTrustOptions()).isNull();
    }

    @Test
    void should_create_vertx_options_with_pkc12_truststore() {
        environment.setProperty("servers[0].ssl.truststore.type", "pkcs12");
        environment.setProperty("servers[0].ssl.truststore.path", "truststore.p12");

        when(keyStoreLoaderManager.create(any(KeyStoreLoaderOptions.class))).thenReturn(mock(KeyStoreLoader.class));

        final VertxHttpServerOptions options = VertxHttpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .build();
        final HttpServerOptions httpServerOptions = options.createHttpServerOptions();

        assertThat(httpServerOptions).isNotNull();
        assertThat(httpServerOptions.getPemTrustOptions()).isNull();
        assertThat(httpServerOptions.getPfxTrustOptions()).isNotNull();
    }

    @Test
    void should_create_vertx_options_without_keystore_and_truststore_when_not_secured() {
        final VertxHttpServerOptions options = VertxHttpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .secured(false)
            .build();
        final HttpServerOptions httpServerOptions = options.createHttpServerOptions();

        assertThat(httpServerOptions).isNotNull();
        assertThat(httpServerOptions.getKeyCertOptions()).isNull();
        assertThat(httpServerOptions.getTrustOptions()).isNull();
        assertThat(httpServerOptions.isSsl()).isFalse();
    }

    @Test
    void should_create_vertx_options_without_websocket_options_when_websocket_disabled() {
        final VertxHttpServerOptions options = VertxHttpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .secured(false)
            .websocketEnabled(false)
            .build();
        final HttpServerOptions httpServerOptions = options.createHttpServerOptions();

        assertThat(httpServerOptions.getWebSocketSubProtocols()).isNull();
        assertThat(httpServerOptions.getPerMessageWebSocketCompressionSupported()).isFalse();
        assertThat(httpServerOptions.getPerFrameWebSocketCompressionSupported()).isFalse();
        assertThat(httpServerOptions.getMaxWebSocketMessageSize()).isEqualTo(Integer.valueOf(DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE));
        assertThat(httpServerOptions.getMaxWebSocketFrameSize()).isEqualTo(Integer.valueOf(DEFAULT_MAX_WEBSOCKET_FRAME_SIZE));
    }
}
