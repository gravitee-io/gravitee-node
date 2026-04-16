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

package io.gravitee.node.plugin.cache.redis.configuration;

import io.gravitee.plugin.configurations.redis.HostAndPort;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.redis.RedisSentinelOptions;
import io.gravitee.plugin.configurations.ssl.KeyStore;
import io.gravitee.plugin.configurations.ssl.KeyStoreType;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.TrustStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.gravitee.plugin.configurations.ssl.none.NoneKeyStore;
import io.gravitee.plugin.configurations.ssl.none.NoneTrustStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12TrustStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Reads {@code cache.redis.*} properties and produces a {@link RedisClientOptions}
 * consumable by {@link io.gravitee.node.vertx.client.redis.VertxRedisClientFactory}.
 *
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConfigurationProvider {

    public static final String SENTINEL_PREFIX = ".sentinel";
    public static final String HOST_KEY = "host";
    public static final String PASSWORD_KEY = "password";
    public static final String PORT_KEY = "port";
    public static final String MAX_POOL_SIZE_KEY = "maxPoolSize";
    public static final String MAX_POOL_WAITING_KEY = "maxPoolWaiting";
    public static final String POOL_CLEANER_INTERVAL_KEY = "poolCleanerInterval";
    public static final String POOL_RECYCLE_TIMEOUT_KEY = "poolRecycleTimeout";
    public static final String MAX_WAITING_HANDLERS_KEY = "maxWaitingHandlers";

    private RedisConfigurationProvider() {
        // no op
    }

    public static RedisClientOptions from(Environment environment, String propertiesPrefix) {
        final RedisClientOptions.RedisClientOptionsBuilder builder = RedisClientOptions.builder();

        builder.host(environment.getProperty(propertiesPrefix + "." + HOST_KEY, String.class, RedisClientOptions.DEFAULT_HOST));
        builder.port(environment.getProperty(propertiesPrefix + "." + PORT_KEY, Integer.class, RedisClientOptions.DEFAULT_PORT));
        builder.password(environment.getProperty(propertiesPrefix + "." + PASSWORD_KEY, String.class));

        final boolean useSsl = environment.getProperty(propertiesPrefix + ".ssl", boolean.class, false);
        builder.useSsl(useSsl);

        if (isSentinelEnabled(environment, propertiesPrefix)) {
            final RedisSentinelOptions.RedisSentinelOptionsBuilder sentinelBuilder = RedisSentinelOptions.builder();
            sentinelBuilder.masterId(environment.getProperty(propertiesPrefix + SENTINEL_PREFIX + ".master", String.class, "mymaster"));
            sentinelBuilder.password(environment.getProperty(propertiesPrefix + SENTINEL_PREFIX + "." + PASSWORD_KEY, String.class));
            sentinelBuilder.nodes(getSentinelNodes(environment, propertiesPrefix));
            builder.sentinel(sentinelBuilder.build());
        }

        if (useSsl) {
            builder.ssl(loadSslOptions(environment, propertiesPrefix));
        }

        ofIntProperty(environment, propertiesPrefix + "." + MAX_POOL_SIZE_KEY).ifPresent(builder::maxPoolSize);
        ofIntProperty(environment, propertiesPrefix + "." + MAX_POOL_WAITING_KEY).ifPresent(builder::maxPoolWaiting);
        ofIntProperty(environment, propertiesPrefix + "." + POOL_CLEANER_INTERVAL_KEY).ifPresent(builder::poolCleanerInterval);
        ofIntProperty(environment, propertiesPrefix + "." + POOL_RECYCLE_TIMEOUT_KEY).ifPresent(builder::poolRecycleTimeout);
        ofIntProperty(environment, propertiesPrefix + "." + MAX_WAITING_HANDLERS_KEY).ifPresent(builder::maxWaitingHandlers);

        return builder.build();
    }

    private static java.util.Optional<Integer> ofIntProperty(Environment environment, String key) {
        return java.util.Optional.ofNullable(environment.getProperty(key, Integer.class));
    }

    private static SslOptions loadSslOptions(Environment environment, String propertiesPrefix) {
        final SslOptions sslConfig = new SslOptions();
        sslConfig.setTrustAll(environment.getProperty(propertiesPrefix + ".trustAll", Boolean.class, false));
        // hostnameVerifier: plugin-common-configurations stores a boolean; legacy YAML uses an
        // algorithm name. Map "NONE" (or absent) to false, any other value to true.
        final String hostnameAlgo = environment.getProperty(propertiesPrefix + ".hostnameVerificationAlgorithm", String.class, "NONE");
        sslConfig.setHostnameVerifier(!"NONE".equalsIgnoreCase(hostnameAlgo));
        sslConfig.setKeyStore(loadKeyStore(environment, propertiesPrefix + ".keystore"));
        sslConfig.setTrustStore(loadTrustStore(environment, propertiesPrefix + ".truststore"));
        return sslConfig;
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
            nodes.add(HostAndPort.builder().host(host).port(port).build());
        }
        return nodes;
    }

    private static String nodeAtIndex(int idx) {
        return ".nodes[" + idx + "]";
    }
}
