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

import io.gravitee.node.management.http.vertx.configuration.HttpServerConfiguration;
import io.gravitee.node.management.http.vertx.auth.BasicAuthProvider;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class HttpServerSpringConfiguration {

    @Bean("managementRouter")
    public Router router(Vertx vertx) {
        return Router.router(vertx);
    }

    @Bean("managementHttpServer")
    public HttpServer httpServer(Vertx vertx) {
        HttpServerOptions options =
            new HttpServerOptions()
                .setPort(httpServerConfiguration().getPort())
                .setHost(httpServerConfiguration().getHost());

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
