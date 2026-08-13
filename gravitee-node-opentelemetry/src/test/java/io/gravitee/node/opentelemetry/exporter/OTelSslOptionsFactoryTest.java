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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import java.util.Base64;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OTelSslOptionsFactoryTest {

    private static final String STORE_BYTES = "binary-data";
    private static final String STORE_CONTENT = Base64.getEncoder().encodeToString(STORE_BYTES.getBytes());

    MockEnvironment environment = new MockEnvironment();

    @Test
    void should_build_jks_keystore_from_content_when_path_is_not_set() {
        var result = factory(configuration().keystoreType("JKS").keystoreContent(STORE_CONTENT).keystorePassword("secret").build())
            .buildKeyCertOptions();

        assertThat(result).isInstanceOf(JksOptions.class);
        var jks = (JksOptions) result;
        assertThat(jks.getPath()).isNull();
        assertThat(jks.getValue().getBytes()).isEqualTo(STORE_BYTES.getBytes());
        assertThat(jks.getPassword()).isEqualTo("secret");
    }

    @Test
    void should_build_pkcs12_keystore_from_content_when_path_is_not_set() {
        var result = factory(configuration().keystoreType("PKCS12").keystoreContent(STORE_CONTENT).keystorePassword("secret").build())
            .buildKeyCertOptions();

        assertThat(result).isInstanceOf(PfxOptions.class);
        var pfx = (PfxOptions) result;
        assertThat(pfx.getPath()).isNull();
        assertThat(pfx.getValue().getBytes()).isEqualTo(STORE_BYTES.getBytes());
    }

    @Test
    void should_prefer_keystore_path_over_content() {
        var result = factory(
            configuration().keystoreType("JKS").keystorePath("/path/to/keystore.jks").keystoreContent(STORE_CONTENT).build()
        )
            .buildKeyCertOptions();

        var jks = (JksOptions) result;
        assertThat(jks.getPath()).isEqualTo("/path/to/keystore.jks");
        assertThat(jks.getValue()).isNull();
    }

    @Test
    void should_leave_keystore_empty_when_neither_path_nor_content_is_set() {
        var result = (JksOptions) factory(configuration().keystoreType("JKS").keystorePassword("secret").build()).buildKeyCertOptions();

        assertThat(result).isNotNull();
        assertThat(result.getPath()).isNull();
        assertThat(result.getValue()).isNull();
    }

    @Test
    void should_not_build_keystore_for_an_unknown_type() {
        assertThat(factory(configuration().keystoreType("BOGUS").keystoreContent(STORE_CONTENT).build()).buildKeyCertOptions()).isNull();
    }

    @Test
    void should_not_build_keystore_when_no_type_is_set() {
        assertThat(factory(configuration().build()).buildKeyCertOptions()).isNull();
    }

    @Test
    void should_leave_keystore_empty_when_content_is_not_valid_base64() {
        var result = (JksOptions) factory(configuration().keystoreType("JKS").keystoreContent("not base64!").build()).buildKeyCertOptions();

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isNull();
    }

    @Test
    void should_build_pem_keystore_from_cert_and_key_paths() {
        environment.withProperty("services.opentelemetry.exporter.ssl.keystore.certs[0]", "/path/to/client.cer");
        environment.withProperty("services.opentelemetry.exporter.ssl.keystore.keys[0]", "/path/to/client.key");

        var result = factory(configuration().keystoreType("PEM").build()).buildKeyCertOptions();

        assertThat(result).isInstanceOf(PemKeyCertOptions.class);
        var pem = (PemKeyCertOptions) result;
        assertThat(pem.getCertPaths()).containsExactly("/path/to/client.cer");
        assertThat(pem.getKeyPaths()).containsExactly("/path/to/client.key");
    }

    @Test
    void should_build_jks_truststore_from_content_when_path_is_not_set() {
        var result = factory(configuration().truststoreType("JKS").truststoreContent(STORE_CONTENT).truststorePassword("secret").build())
            .buildTrustOptions();

        assertThat(result).isInstanceOf(JksOptions.class);
        var jks = (JksOptions) result;
        assertThat(jks.getPath()).isNull();
        assertThat(jks.getValue().getBytes()).isEqualTo(STORE_BYTES.getBytes());
        assertThat(jks.getPassword()).isEqualTo("secret");
    }

    @Test
    void should_build_pkcs12_truststore_from_content_when_path_is_not_set() {
        var result = factory(configuration().truststoreType("PKCS12").truststoreContent(STORE_CONTENT).build()).buildTrustOptions();

        assertThat(result).isInstanceOf(PfxOptions.class);
        assertThat(((PfxOptions) result).getValue().getBytes()).isEqualTo(STORE_BYTES.getBytes());
    }

    @Test
    void should_prefer_truststore_path_over_content() {
        var result = factory(
            configuration().truststoreType("JKS").truststorePath("/path/to/truststore.jks").truststoreContent(STORE_CONTENT).build()
        )
            .buildTrustOptions();

        var jks = (JksOptions) result;
        assertThat(jks.getPath()).isEqualTo("/path/to/truststore.jks");
        assertThat(jks.getValue()).isNull();
    }

    @Test
    void should_leave_truststore_empty_when_neither_path_nor_content_is_set() {
        var result = (PfxOptions) factory(configuration().truststoreType("PKCS12").build()).buildTrustOptions();

        assertThat(result).isNotNull();
        assertThat(result.getPath()).isNull();
        assertThat(result.getValue()).isNull();
    }

    @Test
    void should_not_build_truststore_for_an_unknown_type() {
        assertThat(factory(configuration().truststoreType("BOGUS").truststoreContent(STORE_CONTENT).build()).buildTrustOptions()).isNull();
    }

    @Test
    void should_leave_truststore_empty_when_content_is_not_valid_base64() {
        var result = (PfxOptions) factory(configuration().truststoreType("PKCS12").truststoreContent("not base64!").build())
            .buildTrustOptions();

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isNull();
    }

    @Test
    void should_leave_pem_truststore_empty_when_no_path_is_set() {
        var result = (PemTrustOptions) factory(configuration().truststoreType("PEM").build()).buildTrustOptions();

        assertThat(result).isNotNull();
        assertThat(result.getCertPaths()).isEmpty();
    }

    @Test
    void should_build_pem_truststore_from_path() {
        var result = factory(configuration().truststoreType("PEM").truststorePath("/path/to/ca.pem").build()).buildTrustOptions();

        assertThat(result).isInstanceOf(PemTrustOptions.class);
        assertThat(((PemTrustOptions) result).getCertPaths()).containsExactly("/path/to/ca.pem");
    }

    private OpenTelemetryConfiguration.OpenTelemetryConfigurationBuilder configuration() {
        return OpenTelemetryConfiguration.builder().environment(environment);
    }

    private OTelSslOptionsFactory factory(final OpenTelemetryConfiguration configuration) {
        return new OTelSslOptionsFactory(configuration);
    }
}
