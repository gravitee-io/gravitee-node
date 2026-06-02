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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.redis.RedisSentinelOptions;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.gravitee.plugin.configurations.ssl.none.NoneKeyStore;
import io.gravitee.plugin.configurations.ssl.none.NoneTrustStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12TrustStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class RedisConfigurationProviderTest {

    private static final String PREFIX = "cache.redis";

    private final MockEnvironment env = new MockEnvironment();

    @Nested
    class Defaults {

        @Test
        void should_apply_library_defaults_when_no_properties_set() {
            RedisClientOptions options = RedisConfigurationProvider.from(env, PREFIX);

            assertThat(options.getHost()).isEqualTo(RedisClientOptions.DEFAULT_HOST);
            assertThat(options.getPort()).isEqualTo(RedisClientOptions.DEFAULT_PORT);
            assertThat(options.getPassword()).isNull();
            assertThat(options.isUseSsl()).isFalse();
            assertThat(options.getSsl()).isNull();
            assertThat(options.getSentinel()).isNull();
            assertThat(options.getMaxPoolSize()).isEqualTo(RedisClientOptions.DEFAULT_MAX_POOL_SIZE);
            assertThat(options.getMaxPoolWaiting()).isEqualTo(RedisClientOptions.DEFAULT_MAX_POOL_WAITING);
            assertThat(options.getPoolCleanerInterval()).isEqualTo(RedisClientOptions.DEFAULT_POOL_CLEANER_INTERVAL);
            assertThat(options.getPoolRecycleTimeout()).isEqualTo(RedisClientOptions.DEFAULT_POOL_RECYCLE_TIMEOUT);
            assertThat(options.getMaxWaitingHandlers()).isEqualTo(RedisClientOptions.DEFAULT_MAX_WAITING_HANDLERS);
        }

        @Test
        void should_apply_standalone_properties() {
            env.setProperty("cache.redis.host", "redis.example.com");
            env.setProperty("cache.redis.port", "16379");
            env.setProperty("cache.redis.password", "secret");

            RedisClientOptions options = RedisConfigurationProvider.from(env, PREFIX);

            assertThat(options.getHost()).isEqualTo("redis.example.com");
            assertThat(options.getPort()).isEqualTo(16379);
            assertThat(options.getPassword()).isEqualTo("secret");
        }

        @Test
        void should_apply_all_pool_properties_when_present() {
            env.setProperty("cache.redis.maxPoolSize", "12");
            env.setProperty("cache.redis.maxPoolWaiting", "34");
            env.setProperty("cache.redis.poolCleanerInterval", "56");
            env.setProperty("cache.redis.poolRecycleTimeout", "78");
            env.setProperty("cache.redis.maxWaitingHandlers", "90");

            RedisClientOptions options = RedisConfigurationProvider.from(env, PREFIX);

            assertThat(options.getMaxPoolSize()).isEqualTo(12);
            assertThat(options.getMaxPoolWaiting()).isEqualTo(34);
            assertThat(options.getPoolCleanerInterval()).isEqualTo(56);
            assertThat(options.getPoolRecycleTimeout()).isEqualTo(78);
            assertThat(options.getMaxWaitingHandlers()).isEqualTo(90);
        }
    }

    @Nested
    class Sentinel {

        // Regression: the previous implementation called setMaster(...) twice, the second
        // call clobbering the master id with the password value. After the rewrite,
        // masterId and password are populated into distinct fields.
        @Test
        void should_map_master_and_password_to_distinct_fields() {
            env.setProperty("cache.redis.sentinel.nodes[0].host", "sentinel-1");
            env.setProperty("cache.redis.sentinel.nodes[0].port", "26379");
            env.setProperty("cache.redis.sentinel.master", "my-master");
            env.setProperty("cache.redis.sentinel.password", "sentinel-secret");

            RedisSentinelOptions sentinel = RedisConfigurationProvider.from(env, PREFIX).getSentinel();

            assertThat(sentinel).isNotNull();
            // enabled must be true: the mapper only activates SENTINEL mode when it is, otherwise
            // the configuration silently degrades to a standalone connection.
            assertThat(sentinel.isEnabled()).isTrue();
            assertThat(sentinel.getMasterId()).isEqualTo("my-master");
            assertThat(sentinel.getPassword()).isEqualTo("sentinel-secret");
        }

        @Test
        void should_default_master_id_to_mymaster_when_absent() {
            env.setProperty("cache.redis.sentinel.nodes[0].host", "sentinel-1");
            env.setProperty("cache.redis.sentinel.nodes[0].port", "26379");

            RedisSentinelOptions sentinel = RedisConfigurationProvider.from(env, PREFIX).getSentinel();

            assertThat(sentinel.getMasterId()).isEqualTo("mymaster");
            assertThat(sentinel.getPassword()).isNull();
        }

        @Test
        void should_collect_indexed_nodes_until_first_gap() {
            env.setProperty("cache.redis.sentinel.nodes[0].host", "sentinel-1");
            env.setProperty("cache.redis.sentinel.nodes[0].port", "26379");
            env.setProperty("cache.redis.sentinel.nodes[1].host", "sentinel-2");
            env.setProperty("cache.redis.sentinel.nodes[1].port", "26380");
            // gap at [2] — loop must stop here
            env.setProperty("cache.redis.sentinel.nodes[3].host", "sentinel-4");
            env.setProperty("cache.redis.sentinel.nodes[3].port", "26381");

            RedisSentinelOptions sentinel = RedisConfigurationProvider.from(env, PREFIX).getSentinel();

            assertThat(sentinel.getNodes()).hasSize(2);
            assertThat(sentinel.getNodes())
                .extracting("host", "port")
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple("sentinel-1", 26379),
                    org.assertj.core.groups.Tuple.tuple("sentinel-2", 26380)
                );
        }

        @Test
        void should_not_set_sentinel_when_no_nodes_configured() {
            // No sentinel.nodes[0].host means sentinel is disabled even if master is set.
            env.setProperty("cache.redis.sentinel.master", "my-master");

            RedisClientOptions options = RedisConfigurationProvider.from(env, PREFIX);

            assertThat(options.getSentinel()).isNull();
        }
    }

    @Nested
    class Ssl {

        @Test
        void should_skip_ssl_options_when_ssl_disabled() {
            env.setProperty("cache.redis.ssl", "false");
            env.setProperty("cache.redis.trustAll", "true");
            env.setProperty("cache.redis.openssl", "true");

            RedisClientOptions options = RedisConfigurationProvider.from(env, PREFIX);

            assertThat(options.isUseSsl()).isFalse();
            assertThat(options.getSsl()).isNull();
        }

        @Test
        void should_set_trust_all_and_default_keystores_when_ssl_enabled() {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.trustAll", "true");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl).isNotNull();
            assertThat(ssl.isTrustAll()).isTrue();
            assertThat(ssl.getKeyStore()).isInstanceOf(NoneKeyStore.class);
            assertThat(ssl.getTrustStore()).isInstanceOf(NoneTrustStore.class);
        }

        // Regression: the old YAML key cache.redis.openssl was honoured but the
        // initial rewrite dropped it. The factory still consumes SslOptions#isOpenSsl,
        // so this property must continue to flow through.
        @Test
        void should_propagate_openssl_to_ssl_options() {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.openssl", "true");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.isOpenSsl()).isTrue();
        }

        @Test
        void should_default_openssl_to_false_when_absent() {
            env.setProperty("cache.redis.ssl", "true");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.isOpenSsl()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "NONE", "none", "None" })
        void should_disable_hostname_verifier_for_NONE_algorithm(String algo) {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.hostnameVerificationAlgorithm", algo);

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.isHostnameVerifier()).isFalse();
        }

        @Test
        void should_disable_hostname_verifier_when_algorithm_absent() {
            env.setProperty("cache.redis.ssl", "true");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.isHostnameVerifier()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "HTTPS", "https", "LDAPS" })
        void should_enable_hostname_verifier_for_non_NONE_algorithm(String algo) {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.hostnameVerificationAlgorithm", algo);

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.isHostnameVerifier()).isTrue();
        }

        @Test
        void should_load_jks_keystore_and_truststore() {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.keystore.type", "JKS");
            env.setProperty("cache.redis.keystore.path", "/etc/redis/keystore.jks");
            env.setProperty("cache.redis.keystore.password", "kspass");
            env.setProperty("cache.redis.keystore.alias", "redis");
            env.setProperty("cache.redis.truststore.type", "JKS");
            env.setProperty("cache.redis.truststore.path", "/etc/redis/truststore.jks");
            env.setProperty("cache.redis.truststore.password", "tspass");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.getKeyStore())
                .isInstanceOfSatisfying(
                    JKSKeyStore.class,
                    ks -> {
                        assertThat(ks.getPath()).isEqualTo("/etc/redis/keystore.jks");
                        assertThat(ks.getPassword()).isEqualTo("kspass");
                        assertThat(ks.getAlias()).isEqualTo("redis");
                    }
                );
            assertThat(ssl.getTrustStore())
                .isInstanceOfSatisfying(
                    JKSTrustStore.class,
                    ts -> {
                        assertThat(ts.getPath()).isEqualTo("/etc/redis/truststore.jks");
                        assertThat(ts.getPassword()).isEqualTo("tspass");
                    }
                );
        }

        @Test
        void should_load_pkcs12_keystore_and_truststore() {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.keystore.type", "pkcs12");
            env.setProperty("cache.redis.keystore.path", "/etc/redis/keystore.p12");
            env.setProperty("cache.redis.truststore.type", "PKCS12");
            env.setProperty("cache.redis.truststore.path", "/etc/redis/truststore.p12");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.getKeyStore()).isInstanceOf(PKCS12KeyStore.class);
            assertThat(ssl.getTrustStore()).isInstanceOf(PKCS12TrustStore.class);
        }

        @Test
        void should_load_pem_keystore_with_key_path_and_cert_path() {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.keystore.type", "PEM");
            env.setProperty("cache.redis.keystore.path", "/etc/redis/client.crt");
            env.setProperty("cache.redis.keystore.keyPath", "/etc/redis/client.key");
            env.setProperty("cache.redis.truststore.type", "PEM");
            env.setProperty("cache.redis.truststore.path", "/etc/redis/ca.crt");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.getKeyStore())
                .isInstanceOfSatisfying(
                    PEMKeyStore.class,
                    ks -> {
                        assertThat(ks.getCertPath()).isEqualTo("/etc/redis/client.crt");
                        assertThat(ks.getKeyPath()).isEqualTo("/etc/redis/client.key");
                    }
                );
            assertThat(ssl.getTrustStore())
                .isInstanceOfSatisfying(PEMTrustStore.class, ts -> assertThat(ts.getPath()).isEqualTo("/etc/redis/ca.crt"));
        }

        @Test
        void should_return_none_keystore_when_path_missing() {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.keystore.type", "JKS");
            // no path -> NoneKeyStore

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.getKeyStore()).isInstanceOf(NoneKeyStore.class);
            assertThat(ssl.getTrustStore()).isInstanceOf(NoneTrustStore.class);
        }

        @Test
        void should_return_none_keystore_when_type_unknown() {
            env.setProperty("cache.redis.ssl", "true");
            env.setProperty("cache.redis.keystore.type", "UNKNOWN");
            env.setProperty("cache.redis.keystore.path", "/etc/redis/keystore");

            SslOptions ssl = RedisConfigurationProvider.from(env, PREFIX).getSsl();

            assertThat(ssl.getKeyStore()).isInstanceOf(NoneKeyStore.class);
        }
    }

    @Nested
    class PropertyPrefix {

        @Test
        void should_read_from_supplied_prefix_only() {
            env.setProperty("cache.redis.host", "cache-host");
            env.setProperty("ratelimit.redis.host", "rl-host");

            assertThat(RedisConfigurationProvider.from(env, "cache.redis").getHost()).isEqualTo("cache-host");
            assertThat(RedisConfigurationProvider.from(env, "ratelimit.redis").getHost()).isEqualTo("rl-host");
        }
    }
}
