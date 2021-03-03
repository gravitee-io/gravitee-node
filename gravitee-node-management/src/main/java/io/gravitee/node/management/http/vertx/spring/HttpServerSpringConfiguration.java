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
package io.gravitee.node.management.http.vertx.spring;

import io.gravitee.node.management.http.vertx.auth.BasicAuthProvider;
import io.gravitee.node.management.http.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class HttpServerSpringConfiguration {

    private static final String CERTIFICATE_FORMAT_JKS = "JKS";
    private static final String CERTIFICATE_FORMAT_PEM = "PEM";
    private static final String CERTIFICATE_FORMAT_PKCS12 = "PKCS12";

    @Bean("managementRouter")
    public Router router(Vertx vertx) {
        return Router.router(vertx);
    }

    @Bean("managementWebhookRouter")
    public Router webHookRouter(Vertx vertx) {
        return Router.router(vertx);
    }

    @Bean("managementHttpServer")
    public HttpServer httpServer(Vertx vertx) {
        HttpServerOptions options =
            new HttpServerOptions()
                .setPort(httpServerConfiguration().getPort())
                .setHost(httpServerConfiguration().getHost());

        if (httpServerConfiguration().isSecured()) {
            options.setSsl(httpServerConfiguration().isSecured());
            options.setUseAlpn(httpServerConfiguration().isAlpn());

            String clientAuth = httpServerConfiguration().getClientAuth();
            if (!StringUtils.isEmpty(clientAuth)) {
                if (clientAuth.equalsIgnoreCase(Boolean.TRUE.toString())) {
                    options.setClientAuth(ClientAuth.REQUIRED);
                } else {
                    options.setClientAuth(ClientAuth.NONE);
                }
            } else {
                options.setClientAuth(ClientAuth.REQUEST);
            }

            if (httpServerConfiguration().getTrustStorePath() != null) {
                if (httpServerConfiguration().getTrustStoreType() == null || httpServerConfiguration().getTrustStoreType().isEmpty() ||
                        httpServerConfiguration().getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)) {
                    options.setTrustStoreOptions(new JksOptions()
                            .setPath(httpServerConfiguration().getTrustStorePath())
                            .setPassword(httpServerConfiguration().getTrustStorePassword()));
                } else if (httpServerConfiguration().getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    options.setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(httpServerConfiguration().getTrustStorePath()));
                } else if (httpServerConfiguration().getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxTrustOptions(new PfxOptions()
                            .setPath(httpServerConfiguration().getTrustStorePath())
                            .setPassword(httpServerConfiguration().getTrustStorePassword()));
                }
            }

            if (httpServerConfiguration().getKeyStorePath() != null) {
                if (httpServerConfiguration().getKeyStoreType() == null || httpServerConfiguration().getKeyStoreType().isEmpty() ||
                        httpServerConfiguration().getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)) {
                    options.setKeyStoreOptions(new JksOptions()
                            .setPath(httpServerConfiguration().getKeyStorePath())
                            .setPassword(httpServerConfiguration().getKeyStorePassword()));
                } else if (httpServerConfiguration().getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    options.setPemKeyCertOptions(new PemKeyCertOptions()
                            .addCertPath(httpServerConfiguration().getKeyStorePath()));
                } else if (httpServerConfiguration().getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxKeyCertOptions(new PfxOptions()
                            .setPath(httpServerConfiguration().getKeyStorePath())
                            .setPassword(httpServerConfiguration().getKeyStorePassword()));
                }
            }
        }
        options.setIdleTimeout(httpServerConfiguration().getIdleTimeout());

        return vertx.createHttpServer(options);
    }

    @Bean("managementAuthProvider")
    public AuthProvider authProvider() {
        return new BasicAuthProvider();
    }

    @Bean("managementHttpServerConfiguration")
    public HttpServerConfiguration httpServerConfiguration() {
        return new HttpServerConfiguration();
    }
}
