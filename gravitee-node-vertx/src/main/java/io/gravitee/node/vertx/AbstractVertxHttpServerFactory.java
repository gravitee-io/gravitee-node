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
package io.gravitee.node.vertx;

import static io.gravitee.node.api.certificate.KeyStoreLoader.*;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.vertx.cert.VertxCertificateManager;
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractVertxHttpServerFactory<T> implements FactoryBean<T> {

    private final HttpServerConfiguration httpServerConfiguration;
    private final KeyStoreLoaderManager keyStoreLoaderManager;

    public AbstractVertxHttpServerFactory(HttpServerConfiguration httpServerConfiguration, KeyStoreLoaderManager keyStoreLoaderManager) {
        this.httpServerConfiguration = httpServerConfiguration;
        this.keyStoreLoaderManager = keyStoreLoaderManager;
    }

    protected HttpServerOptions getHttpServerOptions() {
        HttpServerOptions options = new HttpServerOptions();
        options.setTracingPolicy(httpServerConfiguration.getTracingPolicy());

        // Binding port
        options.setPort(httpServerConfiguration.getPort());
        options.setHost(httpServerConfiguration.getHost());

        if (httpServerConfiguration.isSecured()) {
            if (keyStoreLoaderManager == null) {
                throw new IllegalArgumentException("You must provide a KeyStoreLoaderManager when 'secured' is enabled.");
            }

            if (httpServerConfiguration.isOpenssl()) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            options.setSsl(httpServerConfiguration.isSecured());
            options.setUseAlpn(httpServerConfiguration.isAlpn());
            options.setSni(httpServerConfiguration.isSni());

            // TLS protocol support
            if (httpServerConfiguration.getTlsProtocols() != null) {
                options.setEnabledSecureTransportProtocols(
                    new HashSet<>(Arrays.asList(httpServerConfiguration.getTlsProtocols().split("\\s*,\\s*")))
                );
            }

            // restrict the authorized ciphers
            if (httpServerConfiguration.getAuthorizedTlsCipherSuites() != null) {
                httpServerConfiguration.getAuthorizedTlsCipherSuites().stream().map(String::trim).forEach(options::addEnabledCipherSuite);
            }

            options.setClientAuth(httpServerConfiguration.getClientAuth());

            if (httpServerConfiguration.getTrustStorePaths() != null && !httpServerConfiguration.getTrustStorePaths().isEmpty()) {
                if (
                    httpServerConfiguration.getTrustStoreType() == null ||
                    httpServerConfiguration.getTrustStoreType().isEmpty() ||
                    httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)
                ) {
                    options.setTrustStoreOptions(
                        new JksOptions()
                            .setPath(httpServerConfiguration.getTrustStorePaths().get(0))
                            .setPassword(httpServerConfiguration.getTrustStorePassword())
                    );
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    final PemTrustOptions pemTrustOptions = new PemTrustOptions();
                    httpServerConfiguration.getTrustStorePaths().forEach(pemTrustOptions::addCertPath);
                    options.setPemTrustOptions(pemTrustOptions);
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxTrustOptions(
                        new PfxOptions()
                            .setPath(httpServerConfiguration.getTrustStorePaths().get(0))
                            .setPassword(httpServerConfiguration.getTrustStorePassword())
                    );
                }
            } else if (CERTIFICATE_FORMAT_SELF_SIGNED.equalsIgnoreCase(httpServerConfiguration.getTrustStoreType())) {
                options.setPemTrustOptions(SelfSignedCertificate.create().trustOptions());
            }

            final KeyStoreLoaderOptions keyStoreLoaderOptions = KeyStoreLoaderOptions
                .builder()
                .withKeyStorePath(httpServerConfiguration.getKeyStorePath())
                .withKeyStorePassword(httpServerConfiguration.getKeyStorePassword())
                .withKeyStoreType(httpServerConfiguration.getKeyStoreType())
                .withKeyStoreCertificates(httpServerConfiguration.getKeyStoreCertificates())
                .withKubernetesLocations(httpServerConfiguration.getKeystoreKubernetes())
                .withWatch(true) // TODO: allow to configure watch (globally, just for keystore, ...) ?
                .withDefaultAlias(httpServerConfiguration.getKeyStoreDefaultAlias())
                .build();

            final VertxCertificateManager certificateManager = new VertxCertificateManager(httpServerConfiguration.isSni());
            final KeyStoreLoader keyStoreLoader = keyStoreLoaderManager.create(keyStoreLoaderOptions);
            certificateManager.registerLoader(keyStoreLoader);
            options.setKeyCertOptions(certificateManager.getKeyCertOptions());
            keyStoreLoader.start();
        }

        if (httpServerConfiguration.isProxyProtocol()) {
            options.setUseProxyProtocol(true).setProxyProtocolTimeout(httpServerConfiguration.getProxyProtocolTimeout());
        }

        // Customizable configuration
        options.setHandle100ContinueAutomatically(httpServerConfiguration.isHandle100Continue());
        options.setCompressionSupported(httpServerConfiguration.isCompressionSupported());
        options.setIdleTimeout(httpServerConfiguration.getIdleTimeout());
        options.setTcpKeepAlive(httpServerConfiguration.isTcpKeepAlive());
        options.setMaxChunkSize(httpServerConfiguration.getMaxChunkSize());
        options.setMaxHeaderSize(httpServerConfiguration.getMaxHeaderSize());
        options.setMaxInitialLineLength(httpServerConfiguration.getMaxInitialLineLength());
        options.setMaxFormAttributeSize(httpServerConfiguration.getMaxFormAttributeSize());

        // Configure websocket
        System.setProperty("vertx.disableWebsockets", Boolean.toString(!httpServerConfiguration.isWebsocketEnabled()));
        if (httpServerConfiguration.isWebsocketEnabled() && httpServerConfiguration.getWebsocketSubProtocols() != null) {
            options.setWebSocketSubProtocols(
                new ArrayList<>(Arrays.asList(httpServerConfiguration.getWebsocketSubProtocols().split("\\s*,\\s*")))
            );
            options.setPerMessageWebSocketCompressionSupported(httpServerConfiguration.isPerMessageWebSocketCompressionSupported());
            options.setPerFrameWebSocketCompressionSupported(httpServerConfiguration.isPerFrameWebSocketCompressionSupported());
        }

        return options;
    }
}
