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
package io.gravitee.node.management.http.vertx.configuration;

import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.http.HttpServerOptions;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class HttpServerConfiguration {

    private final Configuration configuration;

    public boolean isEnabled() {
        return configuration.getProperty("services.core.http.enabled", Boolean.class, true);
    }

    public int getPort() {
        return configuration.getProperty("services.core.http.port", Integer.class, 18082);
    }

    public String getHost() {
        return configuration.getProperty("services.core.http.host", "localhost");
    }

    public String getAuthenticationType() {
        return configuration.getProperty("services.core.http.authentication.type", "basic");
    }

    public boolean isSecured() {
        return configuration.getProperty("services.core.http.secured", Boolean.class, false);
    }

    public boolean isAlpn() {
        return configuration.getProperty("services.core.http.alpn", Boolean.class, false);
    }

    public String getTlsProtocols() {
        return configuration.getProperty("services.core.http.ssl.tlsProtocols");
    }

    public String getTlsCiphers() {
        return configuration.getProperty("services.core.http.ssl.tlsCiphers");
    }

    public String getKeyStorePath() {
        return configuration.getProperty("services.core.http.ssl.keystore.path");
    }

    public String getKeyStorePassword() {
        return configuration.getProperty("services.core.http.ssl.keystore.password");
    }

    public String getKeyStoreType() {
        return configuration.getProperty("services.core.http.ssl.keystore.type");
    }

    public String getTrustStorePath() {
        return configuration.getProperty("services.core.http.ssl.truststore.path");
    }

    public String getTrustStorePassword() {
        return configuration.getProperty("services.core.http.ssl.truststore.password");
    }

    public String getTrustStoreType() {
        return configuration.getProperty("services.core.http.ssl.truststore.type");
    }

    public int getIdleTimeout() {
        return configuration.getProperty("services.core.http.idleTimeout", Integer.class, HttpServerOptions.DEFAULT_IDLE_TIMEOUT);
    }

    /**
     * null  : REQUEST
     * true  : REQUIRED
     * false : NONE
     */
    public String getClientAuth() {
        return configuration.getProperty("services.core.http.ssl.clientAuth");
    }
}
