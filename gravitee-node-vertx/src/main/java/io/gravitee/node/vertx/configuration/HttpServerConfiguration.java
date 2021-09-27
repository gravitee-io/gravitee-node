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
package io.gravitee.node.vertx.configuration;

import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpServerConfiguration {

  private static final String CERTIFICATE_FORMAT_JKS = "JKS";
  private static final String CERTIFICATE_FORMAT_PEM = "PEM";
  private static final String CERTIFICATE_FORMAT_PKCS12 = "PKCS12";

  private final int port;
  private final String host;
  private final String authenticationType;
  private final boolean secured;
  private final boolean alpn;
  private final boolean sni;
  private final String tlsProtocols;
  private final String keyStorePath;
  private final String keyStorePassword;
  private final String keyStoreType;
  private final String trustStorePath;
  private final String trustStorePassword;
  private final String trustStoreType;
  private final boolean handle100Continue;
  private final boolean compressionSupported;
  private final int idleTimeout;
  private final boolean tcpKeepAlive;
  private final int maxHeaderSize;
  private final int maxChunkSize;
  private final int maxInitialLineLength;
  private final int maxFormAttributeSize;
  private final boolean websocketEnabled;
  private final String websocketSubProtocols;
  private final boolean perMessageWebSocketCompressionSupported;
  private final boolean perFrameWebSocketCompressionSupported;
  private final boolean proxyProtocol;
  private final long proxyProtocolTimeout;
  private final ClientAuth clientAuth;
  private final List<String> authorizedTlsCipherSuites;

  private HttpServerConfiguration(HttpServerConfigurationBuilder builder) {
    this.port = builder.port;
    this.host = builder.host;
    this.authenticationType = builder.authenticationType;
    this.secured = builder.secured;
    this.alpn = builder.alpn;
    this.sni = builder.sni;
    this.tlsProtocols = builder.tlsProtocols;
    this.keyStorePath = builder.keyStorePath;
    this.keyStorePassword = builder.keyStorePassword;
    this.keyStoreType = builder.keyStoreType;
    this.trustStorePath = builder.trustStorePath;
    this.trustStorePassword = builder.trustStorePassword;
    this.trustStoreType = builder.trustStoreType;
    this.handle100Continue = builder.handle100Continue;
    this.compressionSupported = builder.compressionSupported;
    this.idleTimeout = builder.idleTimeout;
    this.tcpKeepAlive = builder.tcpKeepAlive;
    this.maxHeaderSize = builder.maxHeaderSize;
    this.maxChunkSize = builder.maxChunkSize;
    this.maxInitialLineLength = builder.maxInitialLineLength;
    this.maxFormAttributeSize = builder.maxFormAttributeSize;
    this.websocketEnabled = builder.websocketEnabled;
    this.websocketSubProtocols = builder.websocketSubProtocols;
    this.perMessageWebSocketCompressionSupported =
      builder.perMessageWebSocketCompressionSupported;
    this.perFrameWebSocketCompressionSupported =
      builder.perFrameWebSocketCompressionSupported;
    this.proxyProtocol = builder.proxyProtocol;
    this.proxyProtocolTimeout = builder.proxyProtocolTimeout;
    this.clientAuth = builder.clientAuth;
    this.authorizedTlsCipherSuites = builder.authorizedTlsCipherSuites;
  }

  public static HttpServerConfiguration.HttpServerConfigurationBuilder builder() {
    return new HttpServerConfiguration.HttpServerConfigurationBuilder();
  }

  // Property methods
  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

  public String getAuthenticationType() {
    return authenticationType;
  }

  public boolean isSecured() {
    return secured;
  }

  public boolean isAlpn() {
    return alpn;
  }

  public boolean isSni() {
    return sni;
  }

  public String getTlsProtocols() {
    return tlsProtocols;
  }

  public String getKeyStorePath() {
    return keyStorePath;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public String getKeyStoreType() {
    return keyStoreType;
  }

  public String getTrustStorePath() {
    return trustStorePath;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public String getTrustStoreType() {
    return trustStoreType;
  }

  public boolean isHandle100Continue() {
    return handle100Continue;
  }

  public boolean isCompressionSupported() {
    return compressionSupported;
  }

  public int getIdleTimeout() {
    return idleTimeout;
  }

  public boolean isTcpKeepAlive() {
    return tcpKeepAlive;
  }

  public int getMaxHeaderSize() {
    return maxHeaderSize;
  }

  public int getMaxChunkSize() {
    return maxChunkSize;
  }

  public int getMaxInitialLineLength() {
    return maxInitialLineLength;
  }

  public int getMaxFormAttributeSize() {
    return maxFormAttributeSize;
  }

  public boolean isWebsocketEnabled() {
    return websocketEnabled;
  }

  public String getWebsocketSubProtocols() {
    return websocketSubProtocols;
  }

  public boolean isPerMessageWebSocketCompressionSupported() {
    return perMessageWebSocketCompressionSupported;
  }

  public boolean isPerFrameWebSocketCompressionSupported() {
    return perFrameWebSocketCompressionSupported;
  }

  public boolean isProxyProtocol() {
    return proxyProtocol;
  }

  public long getProxyProtocolTimeout() {
    return proxyProtocolTimeout;
  }

  public ClientAuth getClientAuth() {
    return clientAuth;
  }

  public List<String> getAuthorizedTlsCipherSuites() {
    return authorizedTlsCipherSuites;
  }

  public HttpServerOptions getHttpServerOptions() {
    HttpServerOptions options = new HttpServerOptions();

    // Binding port
    options.setPort(this.getPort());
    options.setHost(this.getHost());

    if (this.isSecured()) {
      options.setSsl(this.isSecured());
      options.setUseAlpn(this.isAlpn());
      options.setSni(this.isSni());

      // TLS protocol support
      if (this.getTlsProtocols() != null) {
        options.setEnabledSecureTransportProtocols(
          new HashSet<>(
            Arrays.asList(this.getTlsProtocols().split("\\s*,\\s*"))
          )
        );
      }

      // restrict the authorized ciphers
      if (this.getAuthorizedTlsCipherSuites() != null) {
        this.getAuthorizedTlsCipherSuites()
          .stream()
          .map(String::trim)
          .forEach(options::addEnabledCipherSuite);
      }

      options.setClientAuth(this.getClientAuth());

      if (this.getTrustStorePath() != null) {
        if (
          this.getTrustStoreType() == null ||
          this.getTrustStoreType().isEmpty() ||
          this.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)
        ) {
          options.setTrustStoreOptions(
            new JksOptions()
              .setPath(this.getTrustStorePath())
              .setPassword(this.getTrustStorePassword())
          );
        } else if (
          this.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)
        ) {
          options.setPemTrustOptions(
            new PemTrustOptions().addCertPath(this.getTrustStorePath())
          );
        } else if (
          this.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)
        ) {
          options.setPfxTrustOptions(
            new PfxOptions()
              .setPath(this.getTrustStorePath())
              .setPassword(this.getTrustStorePassword())
          );
        }
      }

      if (this.getKeyStorePath() != null) {
        if (
          this.getKeyStoreType() == null ||
          this.getKeyStoreType().isEmpty() ||
          this.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)
        ) {
          options.setKeyStoreOptions(
            new JksOptions()
              .setPath(this.getKeyStorePath())
              .setPassword(this.getKeyStorePassword())
          );
        } else if (
          this.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)
        ) {
          options.setPemKeyCertOptions(
            new PemKeyCertOptions().addCertPath(this.getKeyStorePath())
          );
        } else if (
          this.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)
        ) {
          options.setPfxKeyCertOptions(
            new PfxOptions()
              .setPath(this.getKeyStorePath())
              .setPassword(this.getKeyStorePassword())
          );
        }
      }
    }

    // Customizable configuration
    options.setHandle100ContinueAutomatically(this.isHandle100Continue());
    options.setCompressionSupported(this.isCompressionSupported());
    options.setIdleTimeout(this.getIdleTimeout());
    options.setTcpKeepAlive(this.isTcpKeepAlive());
    options.setMaxChunkSize(this.getMaxChunkSize());
    options.setMaxHeaderSize(this.getMaxHeaderSize());
    options.setMaxInitialLineLength(this.getMaxInitialLineLength());
    options.setMaxFormAttributeSize(this.getMaxFormAttributeSize());

    // Configure websocket
    System.setProperty(
      "vertx.disableWebsockets",
      Boolean.toString(!this.isWebsocketEnabled())
    );
    if (this.isWebsocketEnabled() && this.getWebsocketSubProtocols() != null) {
      options.setWebSocketSubProtocols(
        new ArrayList<>(
          Arrays.asList(this.getWebsocketSubProtocols().split("\\s*,\\s*"))
        )
      );
      options.setPerMessageWebSocketCompressionSupported(
        this.isPerMessageWebSocketCompressionSupported()
      );
      options.setPerFrameWebSocketCompressionSupported(
        this.isPerFrameWebSocketCompressionSupported()
      );
    }

    return options;
  }

  public static class HttpServerConfigurationBuilder {

    private int port = 8080;
    private String host = "0.0.0.0";
    private String authenticationType;
    private boolean secured;
    private boolean alpn;
    private boolean sni;
    private String tlsProtocols;
    private String keyStorePath;
    private String keyStorePassword;
    private String keyStoreType;
    private String trustStorePath;
    private String trustStorePassword;
    private String trustStoreType;
    private boolean handle100Continue;
    private boolean compressionSupported;
    private int idleTimeout;
    private boolean tcpKeepAlive = true;
    private int maxHeaderSize;
    private int maxChunkSize;
    private int maxInitialLineLength;
    private int maxFormAttributeSize;
    private boolean websocketEnabled;
    private String websocketSubProtocols;
    private boolean perMessageWebSocketCompressionSupported;
    private boolean perFrameWebSocketCompressionSupported;
    private boolean proxyProtocol;
    private long proxyProtocolTimeout;
    private ClientAuth clientAuth;
    private List<String> authorizedTlsCipherSuites;

    private Environment environment;

    private String prefix;

    public HttpServerConfigurationBuilder withDefaultPort(int port) {
      Assert.isTrue(port > 0, "Port should be bigger than 0");
      this.port = port;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultHost(String host) {
      Assert.hasText(host, "Host can't be null or empty");
      this.host = host;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultAuthenticationType(
      String authenticationType
    ) {
      this.authenticationType = authenticationType;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultSecured(boolean secured) {
      this.secured = secured;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultAlpn(boolean alpn) {
      this.alpn = alpn;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultSni(boolean sni) {
      this.sni = sni;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultTlsProtocols(
      String tlsProtocols
    ) {
      this.tlsProtocols = tlsProtocols;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultKeyStorePath(
      String keyStorePath
    ) {
      this.keyStorePath = keyStorePath;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultKeyStorePassword(
      String keyStorePassword
    ) {
      this.keyStorePassword = keyStorePassword;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultKeyStoreType(
      String keyStoreType
    ) {
      this.keyStoreType = keyStoreType;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultTrustStorePath(
      String trustStorePath
    ) {
      this.trustStorePath = trustStorePath;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultTrustStorePassword(
      String trustStorePassword
    ) {
      this.trustStorePassword = trustStorePassword;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultTrustStoreType(
      String trustStoreType
    ) {
      this.trustStoreType = trustStoreType;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultHandle100Continue(
      boolean handle100Continue
    ) {
      this.handle100Continue = handle100Continue;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultCompressionSupported(
      boolean compressionSupported
    ) {
      this.compressionSupported = compressionSupported;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultIdleTimeout(
      int idleTimeout
    ) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultTcpKeepAlive(
      boolean tcpKeepAlive
    ) {
      this.tcpKeepAlive = tcpKeepAlive;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultMaxHeaderSize(
      int maxHeaderSize
    ) {
      this.maxHeaderSize = maxHeaderSize;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultMaxChunkSize(
      int maxChunkSize
    ) {
      this.maxChunkSize = maxChunkSize;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultMaxInitialLineLength(
      int maxInitialLineLength
    ) {
      this.maxInitialLineLength = maxInitialLineLength;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultMaxFormAttributeSize(
      int maxFormAttributeSize
    ) {
      this.maxFormAttributeSize = maxFormAttributeSize;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultWebsocketEnabled(
      boolean websocketEnabled
    ) {
      this.websocketEnabled = websocketEnabled;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultWebsocketSubProtocols(
      String websocketSubProtocols
    ) {
      this.websocketSubProtocols = websocketSubProtocols;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultPerMessageWebSocketCompressionSupported(
      boolean perMessageWebSocketCompressionSupported
    ) {
      this.perMessageWebSocketCompressionSupported =
        perMessageWebSocketCompressionSupported;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultPerFrameWebSocketCompressionSupported(
      boolean perFrameWebSocketCompressionSupported
    ) {
      this.perFrameWebSocketCompressionSupported =
        perFrameWebSocketCompressionSupported;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultProxyProtocol(
      boolean proxyProtocol
    ) {
      this.proxyProtocol = proxyProtocol;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultProxyProtocolTimeout(
      long proxyProtocolTimeout
    ) {
      this.proxyProtocolTimeout = proxyProtocolTimeout;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultClientAuth(
      ClientAuth clientAuth
    ) {
      this.clientAuth = clientAuth;
      return this;
    }

    public HttpServerConfigurationBuilder withDefaultAuthorizedTlsCipherSuites(
      List<String> authorizedTlsCipherSuites
    ) {
      this.authorizedTlsCipherSuites = authorizedTlsCipherSuites;
      return this;
    }

    public HttpServerConfigurationBuilder withEnvironment(
      Environment environment
    ) {
      this.environment = environment;
      return this;
    }

    public HttpServerConfigurationBuilder withPrefix(String prefix) {
      Assert.notNull(prefix, "Prefix can't be null");
      if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
        prefix = prefix + ".";
      }
      this.prefix = prefix;
      return this;
    }

    public HttpServerConfiguration defaultConfig() {
      withDefaultCompressionSupported(
        HttpServerOptions.DEFAULT_COMPRESSION_SUPPORTED
      );
      withDefaultIdleTimeout(HttpServerOptions.DEFAULT_IDLE_TIMEOUT);
      withDefaultMaxHeaderSize(8192);
      withDefaultMaxChunkSize(8192);
      withDefaultMaxInitialLineLength(4096);
      withDefaultPerMessageWebSocketCompressionSupported(true);
      withDefaultPerFrameWebSocketCompressionSupported(true);
      withDefaultProxyProtocolTimeout(10000);

      return withPrefix("http").build();
    }

    public HttpServerConfiguration build() {
      Assert.notNull(
        environment,
        "Environment can not be null. Call withEnvironment method first to configured it"
      );

      this.port =
        Integer.parseInt(
          environment.getProperty(prefix + "port", String.valueOf(port))
        );
      this.host = environment.getProperty(prefix + "host", host);
      this.authenticationType =
        environment.getProperty(prefix + "authentication", authenticationType);
      this.secured =
        Boolean.getBoolean(
          environment.getProperty(prefix + "secured", String.valueOf(secured))
        );
      this.alpn =
        Boolean.getBoolean(
          environment.getProperty(prefix + "alpn", String.valueOf(alpn))
        );
      this.sni =
        Boolean.getBoolean(
          environment.getProperty(prefix + "ssl.sni", String.valueOf(sni))
        );
      this.tlsProtocols =
        environment.getProperty(prefix + "ssl.tlsProtocols", tlsProtocols);
      this.authorizedTlsCipherSuites =
        environment.getProperty(
          prefix + "ssl.tlsProtocols",
          List.class,
          authorizedTlsCipherSuites
        );

      String sClientAuthMode = environment.getProperty(
        "ssl.clientAuth",
        ClientAuth.NONE.name()
      );
      if (sClientAuthMode.equalsIgnoreCase(Boolean.TRUE.toString())) {
        this.clientAuth = ClientAuth.REQUIRED;
      } else if (sClientAuthMode.equalsIgnoreCase(Boolean.FALSE.toString())) {
        this.clientAuth = ClientAuth.NONE;
      } else {
        this.clientAuth = ClientAuth.valueOf(sClientAuthMode.toUpperCase());
      }

      this.keyStoreType =
        environment.getProperty(prefix + "ssl.keystore.type", keyStoreType);
      this.keyStorePath =
        environment.getProperty(prefix + "ssl.keystore.path", keyStorePath);
      this.keyStorePassword =
        environment.getProperty(
          prefix + "ssl.keystore.password",
          keyStorePassword
        );

      this.trustStoreType =
        environment.getProperty(prefix + "ssl.truststore.type", trustStoreType);
      this.trustStorePath =
        environment.getProperty(prefix + "ssl.truststore.path", trustStorePath);
      this.trustStorePassword =
        environment.getProperty(
          prefix + "ssl.truststore.password",
          trustStorePassword
        );

      this.compressionSupported =
        Boolean.getBoolean(
          environment.getProperty(
            prefix + "compressionSupported",
            String.valueOf(compressionSupported)
          )
        );
      this.idleTimeout =
        Integer.parseInt(
          environment.getProperty(
            prefix + "idleTimeout",
            String.valueOf(idleTimeout)
          )
        );
      this.tcpKeepAlive =
        Boolean.getBoolean(
          environment.getProperty(
            prefix + "tcpKeepAlive",
            String.valueOf(tcpKeepAlive)
          )
        );
      this.maxHeaderSize =
        Integer.parseInt(
          environment.getProperty(
            prefix + "maxHeaderSize",
            String.valueOf(maxHeaderSize)
          )
        );
      this.maxChunkSize =
        Integer.parseInt(
          environment.getProperty(
            prefix + "maxChunkSize",
            String.valueOf(maxChunkSize)
          )
        );
      this.maxInitialLineLength =
        Integer.parseInt(
          environment.getProperty(
            prefix + "maxInitialLineLength",
            String.valueOf(maxInitialLineLength)
          )
        );
      this.maxFormAttributeSize =
        Integer.parseInt(
          environment.getProperty(
            prefix + "maxFormAttributeSize",
            String.valueOf(maxFormAttributeSize)
          )
        );
      this.websocketEnabled =
        Boolean.getBoolean(
          environment.getProperty(
            prefix + "websocket.enabled",
            String.valueOf(websocketEnabled)
          )
        );
      this.websocketSubProtocols =
        environment.getProperty(
          prefix + "websocket.subProtocols",
          websocketSubProtocols
        );
      this.perMessageWebSocketCompressionSupported =
        Boolean.getBoolean(
          environment.getProperty(
            prefix + "websocket.perMessageWebSocketCompressionSupported",
            String.valueOf(perMessageWebSocketCompressionSupported)
          )
        );
      this.perFrameWebSocketCompressionSupported =
        Boolean.getBoolean(
          environment.getProperty(
            prefix + "websocket.perFrameWebSocketCompressionSupported",
            String.valueOf(perFrameWebSocketCompressionSupported)
          )
        );
      this.proxyProtocol =
        Boolean.getBoolean(
          environment.getProperty(
            prefix + "haproxy.proxyProtocol",
            String.valueOf(proxyProtocol)
          )
        );
      this.proxyProtocolTimeout =
        Long.parseLong(
          environment.getProperty(
            prefix + "haproxy.proxyProtocolTimeout",
            String.valueOf(proxyProtocolTimeout)
          )
        );

      return new HttpServerConfiguration(this);
    }
  }
}
