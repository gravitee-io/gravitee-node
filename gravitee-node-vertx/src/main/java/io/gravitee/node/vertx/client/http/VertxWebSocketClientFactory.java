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

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.net.URL;
import lombok.Builder;
import lombok.CustomLog;
import lombok.NonNull;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Builder
public class VertxWebSocketClientFactory {

    @NonNull
    private final Vertx vertx;

    @NonNull
    private final Configuration nodeConfiguration;

    private String name;
    private boolean shared;
    private String defaultTarget;
    private VertxHttpProxyOptions proxyOptions;
    private VertxHttpClientOptions httpOptions;
    private SslOptions sslOptions;

    public WebSocketClient createWebSocketClient() {
        if (httpOptions == null) {
            httpOptions = new VertxHttpClientOptions();
        }
        return vertx.createWebSocketClient(createWebSocketClientOptions());
    }

    private WebSocketClientOptions createWebSocketClientOptions() {
        WebSocketClientOptions options = new WebSocketClientOptions();

        options
            .setIdleTimeout((int) (httpOptions.getIdleTimeout() / 1000))
            .setConnectTimeout((int) httpOptions.getConnectTimeout())
            .setMaxConnections(httpOptions.getMaxConcurrentConnections())
            .setTryUsePerFrameCompression(httpOptions.isUseCompression())
            .setTryUsePerMessageCompression(httpOptions.isUseCompression())
            .setCompressionAllowClientNoContext(httpOptions.isUseCompression())
            .setCompressionRequestServerNoContext(httpOptions.isUseCompression());

        final URL target = VertxHttpClientFactory.buildUrl(defaultTarget);

        configureProxy(options);
        configureSsl(options, target);

        if (name != null) {
            options.setName(name);
        }

        return options
            .setShared(shared)
            .setDefaultPort(VertxHttpClientFactory.getPort(target, VertxHttpClientFactory.isSecureProtocol(target.getProtocol())))
            .setDefaultHost(target.getHost());
    }

    private void configureProxy(final WebSocketClientOptions options) {
        if (proxyOptions != null && proxyOptions.isEnabled()) {
            if (proxyOptions.isUseSystemProxy()) {
                setSystemProxy(options);
            } else {
                ProxyOptions vertxProxyOptions = new ProxyOptions();
                vertxProxyOptions.setHost(this.proxyOptions.getHost());
                vertxProxyOptions.setPort(this.proxyOptions.getPort());
                vertxProxyOptions.setUsername(this.proxyOptions.getUsername());
                vertxProxyOptions.setPassword(this.proxyOptions.getPassword());
                vertxProxyOptions.setType(ProxyType.valueOf(this.proxyOptions.getType().name()));
                options.setProxyOptions(vertxProxyOptions);
            }
        }
    }

    private void configureSsl(final WebSocketClientOptions options, final URL target) {
        if (VertxHttpClientFactory.isSecureProtocol(target.getProtocol())) {
            options.setSsl(true);

            if (
                Boolean.TRUE.equals(
                    nodeConfiguration.getProperty(VertxHttpClientFactory.HTTP_SSL_OPENSSL_CONFIGURATION, Boolean.class, false)
                )
            ) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            if (sslOptions != null) {
                options.setVerifyHost(sslOptions.isHostnameVerifier()).setTrustAll(sslOptions.isTrustAll());

                try {
                    // Client truststore configuration (trust server certificate).
                    sslOptions.trustStore().flatMap(TrustStore::trustOptions).ifPresent(options::setTrustOptions);

                    // Client keystore configuration (client certificate for mtls).
                    sslOptions.keyStore().flatMap(KeyStore::keyCertOptions).ifPresent(options::setKeyCertOptions);
                } catch (KeyStore.KeyStoreCertOptionsException | TrustStore.TrustOptionsException e) {
                    throw new IllegalArgumentException(e.getMessage() + " for " + target);
                }
            }
        }

        options.setUseAlpn(true);
    }

    private void setSystemProxy(final WebSocketClientOptions options) {
        try {
            options.setProxyOptions(VertxProxyOptionsUtils.buildProxyOptions(nodeConfiguration));
        } catch (Exception e) {
            log.warn(
                "WebSocketClient (name[{}] target[{}]) requires a system proxy to be defined but some configurations are missing or not well defined: {}",
                name,
                defaultTarget,
                e.getMessage()
            );
            log.warn("Ignoring system proxy");
        }
    }
}
