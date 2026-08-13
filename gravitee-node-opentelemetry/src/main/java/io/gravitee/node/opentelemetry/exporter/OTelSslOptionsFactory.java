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
package io.gravitee.node.opentelemetry.exporter;

import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptionsBase;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.TrustOptions;
import java.util.Base64;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Builds Vert.x TLS trust and key-cert options for the OpenTelemetry exporter.
 *
 * <p>A jks or pkcs12 store is supplied either as a file path or as Base64-encoded content, the path winning
 * when both are set. PEM is path-only.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
final class OTelSslOptionsFactory {

    private static final String KEYSTORE_FORMAT_JKS = "JKS";
    private static final String KEYSTORE_FORMAT_PEM = "PEM";
    private static final String KEYSTORE_FORMAT_PKCS12 = "PKCS12";
    private static final List<String> KNOWN_FORMATS = List.of(KEYSTORE_FORMAT_JKS, KEYSTORE_FORMAT_PEM, KEYSTORE_FORMAT_PKCS12);

    private final OpenTelemetryConfiguration configuration;

    KeyCertOptions buildKeyCertOptions() {
        var type = configuration.getKeystoreType();
        if (type == null) {
            return null;
        }

        if (KEYSTORE_FORMAT_JKS.equalsIgnoreCase(type)) {
            return keystoreOptions(new JksOptions());
        }
        if (KEYSTORE_FORMAT_PKCS12.equalsIgnoreCase(type)) {
            return keystoreOptions(new PfxOptions());
        }
        if (KEYSTORE_FORMAT_PEM.equalsIgnoreCase(type)) {
            return new PemKeyCertOptions()
                .setCertPaths(configuration.getKeystorePemCerts())
                .setKeyPaths(configuration.getKeystorePemKeys());
        }

        log.warn("Keystore will not be configured because type '{}' is not one of {}", type, KNOWN_FORMATS);
        return null;
    }

    TrustOptions buildTrustOptions() {
        var type = configuration.getTruststoreType();
        if (type == null) {
            return null;
        }

        if (KEYSTORE_FORMAT_JKS.equalsIgnoreCase(type)) {
            return truststoreOptions(new JksOptions());
        }
        if (KEYSTORE_FORMAT_PKCS12.equalsIgnoreCase(type)) {
            return truststoreOptions(new PfxOptions());
        }
        if (KEYSTORE_FORMAT_PEM.equalsIgnoreCase(type)) {
            var path = configuration.getTruststorePath();
            if (path == null || path.isEmpty()) {
                log.warn("Truststore has no path set, so it will be left empty");
                return new PemTrustOptions();
            }
            return new PemTrustOptions().addCertPath(path);
        }

        log.warn("Truststore will not be configured because type '{}' is not one of {}", type, KNOWN_FORMATS);
        return null;
    }

    private <T extends KeyStoreOptionsBase> T keystoreOptions(final T options) {
        return applyStore(
            options,
            configuration.getKeystorePath(),
            configuration.getKeystoreContent(),
            configuration.getKeystorePassword(),
            "Keystore"
        );
    }

    private <T extends KeyStoreOptionsBase> T truststoreOptions(final T options) {
        return applyStore(
            options,
            configuration.getTruststorePath(),
            configuration.getTruststoreContent(),
            configuration.getTruststorePassword(),
            "Truststore"
        );
    }

    /**
     * Populates the store from its path, falling back to its Base64-encoded content.
     *
     * <p>When neither is usable the store is returned empty rather than {@code null}: leaving the option unset
     * would let Vert.x fall back to the JVM default trust store, so a truststore the operator asked for but
     * misconfigured would silently widen trust instead of failing.
     */
    private <T extends KeyStoreOptionsBase> T applyStore(
        final T options,
        final String path,
        final String content,
        final String password,
        final String store
    ) {
        options.setPassword(password);

        if (path != null && !path.isEmpty()) {
            options.setPath(path);
            return options;
        }

        if (content != null && !content.isEmpty()) {
            try {
                options.setValue(Buffer.buffer(Base64.getDecoder().decode(content)));
                return options;
            } catch (IllegalArgumentException e) {
                log.warn("{} content is not valid Base64, so it will be left empty: {}", store, e.getMessage());
                return options;
            }
        }

        log.warn("{} has neither path nor content set, so it will be left empty", store);
        return options;
    }
}
