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

package io.gravitee.node.plugin.cache.redis.configuration;

import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.KeyStoreType;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.client.ssl.jks.JKSKeyStore;
import io.gravitee.node.vertx.client.ssl.jks.JKSTrustStore;
import io.gravitee.node.vertx.client.ssl.none.NoneKeyStore;
import io.gravitee.node.vertx.client.ssl.none.NoneTrustStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMKeyStore;
import io.gravitee.node.vertx.client.ssl.pem.PEMTrustStore;
import io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12TrustStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConfigurationProvider {

    public static final String SENTINEL_PREFIX = ".sentinel";
    public static final String HOST_KEY = "host";
    public static final String PASSWORD_KEY = "password";
    public static final String PORT_KEY = "port";

    private RedisConfigurationProvider() {
        // no op
    }

    public static RedisConfiguration from(Environment environment, String propertiesPrefix) {
        RedisConfiguration config = new RedisConfiguration();

        final String host = environment.getProperty(propertiesPrefix + "." + HOST_KEY, String.class, "localhost");
        final Integer port = environment.getProperty(propertiesPrefix + "." + PORT_KEY, Integer.class, 6379);
        final String password = environment.getProperty(propertiesPrefix + "." + PASSWORD_KEY, String.class);
        final boolean useSsl = environment.getProperty(propertiesPrefix + ".ssl", boolean.class, false);

        final var hostAndPort = HostAndPort.of(host, port).withPassword(password).withSsl(useSsl);
        config.setHostAndPort(hostAndPort);

        SentinelConfiguration sentinelConfiguration = new SentinelConfiguration();
        sentinelConfiguration.setEnabled(isSentinelEnabled(environment, propertiesPrefix));
        if (sentinelConfiguration.isEnabled()) {
            sentinelConfiguration.setMaster(
                environment.getProperty(propertiesPrefix + SENTINEL_PREFIX + ".master", String.class, "mymaster")
            );
            sentinelConfiguration.setMaster(environment.getProperty(propertiesPrefix + SENTINEL_PREFIX + "." + PASSWORD_KEY, String.class));

            List<HostAndPort> sentinelNodes = getSentinelNodes(environment, propertiesPrefix);
            sentinelNodes.forEach(node -> node.withPassword(hostAndPort.password()).withSsl(hostAndPort.useSsl()));
            sentinelConfiguration.setNodes(sentinelNodes);
        }
        config.setSentinelConfiguration(sentinelConfiguration);

        if (useSsl) {
            final var useOpenSSL = environment.getProperty(propertiesPrefix + ".openssl", Boolean.class, false);
            final var trustAll = environment.getProperty(propertiesPrefix + ".trustAll", Boolean.class, false);
            final var hostnameVerificationAlgorithm = environment.getProperty(
                propertiesPrefix + ".hostnameVerificationAlgorithm",
                String.class,
                "NONE"
            );

            final var sslConfig = new SslOptions();
            sslConfig.setTrustAll(trustAll);
            sslConfig.setOpenSsl(useOpenSSL);
            sslConfig.setHostnameVerificationAlgorithm(hostnameVerificationAlgorithm);
            sslConfig.setKeyStore(loadKeyStore(environment, propertiesPrefix + ".keystore"));
            sslConfig.setTrustStore(loadTrustStore(environment, propertiesPrefix + ".truststore"));
            config.setSslConfiguration(sslConfig);
        }

        return config;
    }

    private static KeyStore loadKeyStore(Environment environment, String propertiesPrefix) {
        String path = environment.getProperty(propertiesPrefix + ".path", String.class);
        String type = environment.getProperty(propertiesPrefix + ".type", String.class);
        String password = environment.getProperty(propertiesPrefix + "." + PASSWORD_KEY, String.class);
        String keyPassword = environment.getProperty(propertiesPrefix + ".keyPassword", String.class);
        String alias = environment.getProperty(propertiesPrefix + ".alias", String.class);
        String keyPath = environment.getProperty(propertiesPrefix + ".keyPath", String.class);

        if (!StringUtils.hasText(path)) {
            return new NoneKeyStore();
        }

        if (KeyStoreType.PKCS12.name().equalsIgnoreCase(type)) {
            final var keystore = new PKCS12KeyStore();
            keystore.setAlias(alias);
            keystore.setPassword(password);
            keystore.setKeyPassword(keyPassword);
            keystore.setPath(path);
            return keystore;
        }

        if (KeyStoreType.JKS.name().equalsIgnoreCase(type)) {
            final var keystore = new JKSKeyStore();
            keystore.setAlias(alias);
            keystore.setPassword(password);
            keystore.setKeyPassword(keyPassword);
            keystore.setPath(path);
            return keystore;
        }

        if (KeyStoreType.PEM.name().equalsIgnoreCase(type)) {
            final var keystore = new PEMKeyStore();
            keystore.setKeyPath(keyPath);
            keystore.setCertPath(path);
            return keystore;
        }

        return new NoneKeyStore();
    }

    private static TrustStore loadTrustStore(Environment environment, String propertiesPrefix) {
        String path = environment.getProperty(propertiesPrefix + ".path", String.class);
        String type = environment.getProperty(propertiesPrefix + ".type", String.class);
        String password = environment.getProperty(propertiesPrefix + "." + PASSWORD_KEY, String.class);
        String alias = environment.getProperty(propertiesPrefix + ".alias", String.class);

        if (!StringUtils.hasText(path)) {
            return new NoneTrustStore();
        }

        if (KeyStoreType.PKCS12.name().equalsIgnoreCase(type)) {
            final var keystore = new PKCS12TrustStore();
            keystore.setAlias(alias);
            keystore.setPassword(password);
            keystore.setPath(path);
            return keystore;
        }

        if (KeyStoreType.JKS.name().equalsIgnoreCase(type)) {
            final var keystore = new JKSTrustStore();
            keystore.setAlias(alias);
            keystore.setPassword(password);
            keystore.setPath(path);
            return keystore;
        }

        if (KeyStoreType.PEM.name().equalsIgnoreCase(type)) {
            final var keystore = new PEMTrustStore();
            keystore.setPath(path);
            return keystore;
        }

        return new NoneTrustStore();
    }

    private static boolean isSentinelEnabled(Environment environment, String propertyPrefix) {
        return StringUtils.hasLength(
            environment.getProperty(propertyPrefix + SENTINEL_PREFIX + nodeAtIndex(0) + "." + HOST_KEY, String.class)
        );
    }

    private static List<HostAndPort> getSentinelNodes(Environment environment, String propertyPrefix) {
        final List<HostAndPort> nodes = new ArrayList<>();
        for (
            int idx = 0;
            StringUtils.hasText(
                environment.getProperty(propertyPrefix + SENTINEL_PREFIX + nodeAtIndex(idx) + "." + HOST_KEY, String.class)
            );
            idx++
        ) {
            String host = environment.getProperty(propertyPrefix + SENTINEL_PREFIX + nodeAtIndex(idx) + "." + HOST_KEY, String.class);
            Integer port = environment.getProperty(propertyPrefix + SENTINEL_PREFIX + nodeAtIndex(idx) + "." + PORT_KEY, Integer.class, 0);
            nodes.add(HostAndPort.of(host, port));
        }
        return nodes;
    }

    private static String nodeAtIndex(int idx) {
        return ".nodes[" + idx + "]";
    }
}
