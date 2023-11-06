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

import static io.gravitee.node.vertx.server.VertxServerOptions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.NetServerOptions;
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
class VertxTcpServerOptionsTest {

    public static final String SERVER_ID = "test";
    public static final String TCP_KEEP_ALIVE = "true";
    public static final String TRACING_POLICY = "ignore";
    public static final String SECURED = "true";
    public static final String IDLE_TIMEOUT = "5000";
    public static final String HOST = "10.1.1.1";
    public static final String PORT = "7890";
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
            .withProperty("servers[0].tcpKeepAlive", TCP_KEEP_ALIVE)
            .withProperty("servers[0].idleTimeout", IDLE_TIMEOUT)
            .withProperty("servers[0].tracingPolicy", TRACING_POLICY)
            .withProperty("servers[0].host", HOST)
            .withProperty("servers[0].port", PORT)
            .withProperty("servers[0].secured", SECURED)
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
            .withProperty("servers[0].haproxy.proxyProtocol", HAPROXY_PROTOCOL)
            .withProperty("servers[0].haproxy.proxyProtocolTimeout", HAPROXY_PROTOCOL_TIMEOUT);
    }

    @Test
    void should_build_from_environment_configuration() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getId()).isEqualTo(SERVER_ID);
        assertThat(options.isTcpKeepAlive()).isEqualTo(Boolean.valueOf(TCP_KEEP_ALIVE));
        assertThat(options.getIdleTimeout()).isEqualTo(Integer.valueOf(IDLE_TIMEOUT));
        assertThat(options.getHost()).isEqualTo(HOST);
        assertThat(options.getPort()).isEqualTo(Integer.valueOf(PORT));
        assertThat(options.isSecured()).isEqualTo(Boolean.valueOf(SECURED));
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
        assertThat(options.isHaProxyProtocol()).isEqualTo(Boolean.valueOf(HAPROXY_PROTOCOL));
        assertThat(options.getHaProxyProtocolTimeout()).isEqualTo(Long.valueOf(HAPROXY_PROTOCOL_TIMEOUT));
    }

    @Test
    void should_build_from_environment_configuration_with_default_values_when_unknown_prefix() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().prefix("unknown").environment(environment).build();

        assertThat(options.getId()).isNotEqualTo(SERVER_ID); // Server id is generated when not provided.
        assertThat(options.isTcpKeepAlive()).isEqualTo(DEFAULT_TCP_KEEP_ALIVE);
        assertThat(options.getIdleTimeout()).isEqualTo(DEFAULT_IDLE_TIMEOUT);
        assertThat(options.getHost()).isEqualTo(DEFAULT_LISTENING_HOST);
        assertThat(options.getPort()).isEqualTo(DEFAULT_PORT);
        assertThat(options.isSecured()).isEqualTo(DEFAULT_SECURED);
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

        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().prefix("servers[0]").environment(environment).build();

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

        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getTrustStoreType()).isEqualToIgnoringCase("pem");
        assertThat(options.getTrustStorePath()).isNull();
        assertThat(options.getTrustStorePaths()).isEqualTo(List.of("cert1.pem", "cert2.pem"));
        assertThat(options.getTrustStorePassword()).isEqualTo(TRUSTSTORE_PASSWORD);
    }

    @Test
    void should_build_from_environment_configuration_with_client_auth_true() {
        // Support for old version where clientAuth property supported boolean.
        environment.setProperty("servers[0].ssl.clientAuth", "true");

        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getClientAuth()).isEqualToIgnoringCase(ClientAuth.REQUIRED.name());
    }

    @Test
    void should_build_from_environment_configuration_with_client_auth_false() {
        // Support for old version where clientAuth property supported boolean.
        environment.setProperty("servers[0].ssl.clientAuth", "false");

        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().prefix("servers[0]").environment(environment).build();

        assertThat(options.getClientAuth()).isEqualToIgnoringCase(ClientAuth.NONE.name());
    }

    @Test
    void should_throw_illegal_argument_exception_from_environment_configuration_when_prefix_is_not_set() {
        VertxServerOptionsBuilder<?, ?> builder = builder().environment(environment);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        assertThat(exception.getMessage()).isEqualTo("Prefix must be set before environment");
    }

    @Test
    void should_build_with_default() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().build();

        assertThat(options.getId()).isNotEqualTo(SERVER_ID); // Server id is generated when not provided.
        assertThat(options.isCompressionSupported()).isEqualTo(DEFAULT_COMPRESSION_SUPPORTED);
        assertThat(options.isTcpKeepAlive()).isEqualTo(DEFAULT_TCP_KEEP_ALIVE);
        assertThat(options.getIdleTimeout()).isEqualTo(DEFAULT_IDLE_TIMEOUT);
        assertThat(options.getHost()).isEqualTo(DEFAULT_LISTENING_HOST);
        assertThat(options.getPort()).isEqualTo(DEFAULT_PORT);
        assertThat(options.isSecured()).isEqualTo(DEFAULT_SECURED);
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
        assertThat(options.isHaProxyProtocol()).isEqualTo(DEFAULT_HAPROXY_PROTOCOL);
        assertThat(options.getHaProxyProtocolTimeout()).isEqualTo(Long.valueOf(DEFAULT_HAPROXY_PROTOCOL_TIMEOUT));
    }

    @Test
    void should_build_with_default_and_use_specified_default_port_when_no_port_explicitly_set() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().defaultPort(1234).build();

        assertThat(options.getPort()).isEqualTo(1234);
    }

    @Test
    void should_build_with_default_and_not_use_specified_default_port_when_port_explicitly_set() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().port(5678).defaultPort(1234).build();

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

        final VertxTcpServerOptions options = VertxTcpServerOptions
            .builder()
            .defaultPort(9876)
            .prefix("servers[0]")
            .environment(environment)
            .build();

        assertThat(options.getPort()).isEqualTo(9876);
    }

    @Test
    void should_build_with_default_and_not_use_specified_default_port_when_port_explicitly_set_using_environment_configuration() {
        final VertxTcpServerOptions options = VertxTcpServerOptions
            .builder()
            .defaultPort(1234)
            .prefix("servers[0]")
            .environment(environment)
            .build();

        assertThat(options.getPort()).isEqualTo(Integer.valueOf(PORT));
    }

    @Test
    void should_create_vertx_options() {
        final VertxTcpServerOptions options = VertxTcpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .build();

        when(keyStoreLoaderManager.create(any(KeyStoreLoaderOptions.class))).thenReturn(mock(KeyStoreLoader.class));

        final NetServerOptions netServerOptions = options.createNetServerOptions();

        assertThat(netServerOptions).isNotNull();
        assertThat(netServerOptions.isTcpKeepAlive()).isEqualTo(Boolean.valueOf(TCP_KEEP_ALIVE));
        assertThat(netServerOptions.getIdleTimeout()).isEqualTo(Integer.valueOf(IDLE_TIMEOUT));
        assertThat(netServerOptions.getHost()).isEqualTo(HOST);
        assertThat(netServerOptions.getPort()).isEqualTo(Integer.valueOf(PORT));
        assertThat(netServerOptions.getClientAuth()).isEqualTo(ClientAuth.valueOf(CLIENT_AUTH.toUpperCase()));
        assertThat(netServerOptions.getEnabledSecureTransportProtocols()).isEqualTo(Set.of(TLS_PROTOCOLS));
        assertThat(netServerOptions.getEnabledCipherSuites()).isEqualTo(new HashSet<>(Arrays.asList(TLS_CIPHERS.split(",\\s?"))));
        assertThat(netServerOptions.getKeyCertOptions()).isNotNull();
        assertThat(netServerOptions.getTrustOptions()).isNotNull();
        assertThat(netServerOptions.isSsl()).isEqualTo(Boolean.valueOf(SECURED));
        assertThat(netServerOptions.isSni()).isEqualTo(Boolean.valueOf(SNI));
        assertThat(netServerOptions.getOpenSslEngineOptions()).isNotNull();
        assertThat(netServerOptions.isUseProxyProtocol()).isEqualTo(Boolean.valueOf(HAPROXY_PROTOCOL));
        assertThat(netServerOptions.getProxyProtocolTimeout()).isEqualTo(Long.valueOf(HAPROXY_PROTOCOL_TIMEOUT));
    }

    @Test
    void should_throw_illegal_argument_exception_when_create_vertx_options_with_secured_enabled_and_no_keystore_loader_manager() {
        final VertxTcpServerOptions options = VertxTcpServerOptions.builder().prefix("servers[0]").environment(environment).build();
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::createNetServerOptions);

        assertThat(exception.getMessage()).isEqualTo("You must provide a KeyStoreLoaderManager when 'secured' is enabled.");
    }

    @Test
    void should_throw_illegal_argument_exception_when_create_vertx_options_with_unsecured_options() {
        environment.setProperty("servers[0].secured", "false");
        final VertxTcpServerOptions options = VertxTcpServerOptions
            .builder()
            .prefix("servers[0]")
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .environment(environment)
            .build();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::createNetServerOptions);
        assertThat(exception.getMessage()).isEqualTo("Cannot start unsecured TCP server or without SNI enabled");
    }

    @Test
    void should_throw_illegal_argument_exception_when_create_vertx_options_without_SNI() {
        environment.setProperty("servers[0].ssl.sni", "false");
        final VertxTcpServerOptions options = VertxTcpServerOptions
            .builder()
            .prefix("servers[0]")
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .environment(environment)
            .build();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::createNetServerOptions);
        assertThat(exception.getMessage()).isEqualTo("Cannot start unsecured TCP server or without SNI enabled");
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

        final VertxTcpServerOptions options = VertxTcpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .build();
        final NetServerOptions netServerOptions = options.createNetServerOptions();

        assertThat(netServerOptions).isNotNull();
        assertThat(netServerOptions.getPemTrustOptions()).isNotNull();
        assertThat(netServerOptions.getPfxTrustOptions()).isNull();
    }

    @Test
    void should_create_vertx_options_with_pkc12_truststore() {
        environment.setProperty("servers[0].ssl.truststore.type", "pkcs12");
        environment.setProperty("servers[0].ssl.truststore.path", "truststore.p12");

        when(keyStoreLoaderManager.create(any(KeyStoreLoaderOptions.class))).thenReturn(mock(KeyStoreLoader.class));

        final VertxTcpServerOptions options = VertxTcpServerOptions
            .builder()
            .prefix("servers[0]")
            .environment(environment)
            .keyStoreLoaderManager(keyStoreLoaderManager)
            .build();
        final NetServerOptions netServerOptions = options.createNetServerOptions();

        assertThat(netServerOptions).isNotNull();
        assertThat(netServerOptions.getPemTrustOptions()).isNull();
        assertThat(netServerOptions.getPfxTrustOptions()).isNotNull();
    }
}
