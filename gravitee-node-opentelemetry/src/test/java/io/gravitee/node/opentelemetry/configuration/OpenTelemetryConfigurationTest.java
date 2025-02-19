package io.gravitee.node.opentelemetry.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.internal.AttributesMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenTelemetryConfigurationTest {

    MockEnvironment environment = new MockEnvironment();
    OpenTelemetryConfiguration underTest = new OpenTelemetryConfiguration(environment);

    @Test
    void should_get_custom_headers() {
        environment.setProperty("services.opentelemetry.exporter.headers[0].foo", "bar");
        environment.setProperty("services.opentelemetry.exporter.headers[1].baz", "puk");
        Map<String, String> propertyMap = underTest.getCustomHeaders();
        assertThat(propertyMap).containsAllEntriesOf(Map.of("foo", "bar", "baz", "puk"));
    }

    @Test
    void should_get_custom_headers_from_fallback() {
        environment.setProperty("services.tracing.otel.headers[0].foo", "bar");
        environment.setProperty("services.tracing.otel.headers[1].baz", "puk");
        Map<String, String> propertyMap = underTest.getCustomHeaders();
        assertThat(propertyMap).containsAllEntriesOf(Map.of("foo", "bar", "baz", "puk"));
    }

    @Test
    void should_get_empty_custom_headers() {
        Map<String, String> propertyMap = underTest.getCustomHeaders();
        assertThat(propertyMap).isEmpty();
    }

    @Test
    void should_get_extra_attributes() {
        environment.setProperty("services.opentelemetry.extraAttributes[0].foo", "bar");
        environment.setProperty("services.opentelemetry.extraAttributes[1].baz", "puk");
        AttributesMap extraAttributes = underTest.getExtraAttributes();
        assertThat(extraAttributes).isNotEmpty();
        assertThat(extraAttributes.get(AttributeKey.stringKey("foo"))).isEqualTo("bar");
        assertThat(extraAttributes.get(AttributeKey.stringKey("baz"))).isEqualTo("puk");
    }

    @Test
    void should_get_empty_extra_attributes() {
        assertThat(underTest.getExtraAttributes()).isEmpty();
    }

    @Test
    void should_get_cert_and_keys() {
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.certs[0]", "/path/to/cert/1");
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.certs[1]", "/path/to/cert/2");
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.keys[0]", "/path/to/key/1");
        environment.setProperty("services.opentelemetry.exporter.ssl.keystore.keys[1]", "/path/to/key/2");

        assertThat(underTest.getKeystorePemCerts()).containsExactly("/path/to/cert/1", "/path/to/cert/2");
        assertThat(underTest.getKeystorePemKeys()).containsExactly("/path/to/key/1", "/path/to/key/2");
    }

    @Test
    void should_get_cert_and_from_fallback() {
        environment.setProperty("services.tracing.otel.ssl.keystore.certs[0]", "/path/to/cert/1");
        environment.setProperty("services.tracing.otel.ssl.keystore.certs[1]", "/path/to/cert/2");
        environment.setProperty("services.tracing.otel.ssl.keystore.keys[0]", "/path/to/key/1");
        environment.setProperty("services.tracing.otel.ssl.keystore.keys[1]", "/path/to/key/2");

        assertThat(underTest.getKeystorePemCerts()).containsExactly("/path/to/cert/1", "/path/to/cert/2");
        assertThat(underTest.getKeystorePemKeys()).containsExactly("/path/to/key/1", "/path/to/key/2");
    }

    @Test
    void should_get_empty_property_list() {
        assertThat(underTest.getKeystorePemCerts()).isEmpty();
        assertThat(underTest.getKeystorePemKeys()).isEmpty();
    }
}
