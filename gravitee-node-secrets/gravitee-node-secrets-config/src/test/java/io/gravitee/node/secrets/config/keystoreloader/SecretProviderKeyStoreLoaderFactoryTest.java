package io.gravitee.node.secrets.config.keystoreloader;

import static io.gravitee.node.secrets.config.test.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.secrets.config.GraviteeConfigurationSecretResolver;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretProviderKeyStoreLoaderFactoryTest {

    private SecretProviderKeyStoreLoaderFactory cut;

    @BeforeEach
    void before() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        GraviteeConfigurationSecretResolver dispatcher = newDispatcher(pluginManager, env);
        this.cut = new SecretProviderKeyStoreLoaderFactory(dispatcher);
    }

    @ParameterizedTest
    @CsvSource(
        value = {
            "pem, secret://test/test, true",
            "PEM, secret://test/test, true",
            "PKCS12, secret://test/test, true",
            "pkcs12, secret://test/test, true",
            "jks, secret://test/test, true",
            "JKS, secret://test/test, true",
            "foo, secret://test/test, false",
            "JKS, secret://foo/test, false",
            ", secret://test/test, false",
        }
    )
    void should_assess_if_can_handle_url(String type, String url, boolean canHandle) {
        KeyStoreLoaderOptions options = KeyStoreLoaderOptions.builder().type(type).secretLocation(url).build();
        assertThat(cut.canHandle(options)).isEqualTo(canHandle);
    }

    @Test
    void should_create_KeyStoreLoader() {
        KeyStoreLoaderOptions options = KeyStoreLoaderOptions.builder().type("pem").secretLocation("secret://test/test").build();

        KeyStoreLoader keyStoreLoader = cut.create(options);
        assertThat(keyStoreLoader).isInstanceOf(SecretProviderKeyStoreLoader.class);
    }
}
