package io.gravitee.node.secrets.service.conf;

import static io.gravitee.node.secrets.service.test.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.node.api.secrets.errors.SecretManagerConfigurationException;
import io.gravitee.node.api.secrets.errors.SecretProviderNotFoundException;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteeConfigurationSecretResolverDispatcherTest {

    @Test
    void should_create_secret_provider_that_can_handle_resolution() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        GraviteeConfigurationSecretResolverDispatcher cut = newDispatcher(pluginManager, env);

        assertThat(cut.findSecretProvider("foooooooo")).isNotPresent();
        assertThat(cut.findSecretProvider("test")).isPresent();
        assertThat(cut.isEnabled("foooooooo")).isFalse();
        assertThat(cut.isEnabled("test")).isTrue();
        assertThat(cut.enabledProviders()).containsExactly("test");
        assertThat(cut.canHandle("secret://foooooo/test:password")).isFalse();
        assertThat(cut.canHandle("secret://test/test:password")).isTrue();
        assertThat(cut.canResolveSingleValue("secret://foooooo/test:password")).isFalse();
        assertThat(cut.canResolveSingleValue("secret://test/test:password")).isTrue();
    }

    @Test
    void should_create_secret_provider_and_resolve() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        env.setProperty("secrets.test.secrets.password", "noOneWillFindMyPasswordHahahaha");
        GraviteeConfigurationSecretResolverDispatcher cut = newDispatcher(pluginManager, env);
        SecretMount secretMount = cut.toSecretMount("secret://test/test:password");
        assertThat(secretMount.key()).isEqualTo("password");
        assertThat(secretMount.provider()).isEqualTo("test");

        Secret secret = cut.resolveKey(secretMount).blockingGet();
        assertThat(secret).isNotNull();
        assertThat(secret.asString()).isEqualTo("noOneWillFindMyPasswordHahahaha");
        // secret was cached
        assertThat(cut.secrets().get(secretMount.location())).isNotNull();

        SecretMap secretMap = cut.resolve(secretMount).blockingGet();
        assertThat(secretMap).isNotNull();
        assertThat(secretMap.getSecret(secretMount))
            .isPresent()
            .get()
            .extracting(Secret::asString)
            .isEqualTo("noOneWillFindMyPasswordHahahaha");
    }

    @Test
    void should_create_secret_provider_and_watch() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        env.setProperty("secrets.test.secrets.password", "thisIsTheBestPasswordOfAllTime!");
        GraviteeConfigurationSecretResolverDispatcher cut = newDispatcher(pluginManager, env);
        SecretMount secretMount = cut.toSecretMount("secret://test/test:password");

        Secret secret = cut.watchKey(secretMount).blockingFirst();
        assertThat(secret).isNotNull();
        assertThat(secret.asString()).isEqualTo("thisIsTheBestPasswordOfAllTime!");
        // secret was NOT cached
        assertThat(cut.secrets().get(secretMount.location())).isNull();

        SecretMap secretMap = cut.watch(secretMount).blockingFirst();
        assertThat(secretMap).isNotNull();
        assertThat(secretMap.getSecret(secretMount))
            .isPresent()
            .get()
            .extracting(Secret::asString)
            .isEqualTo("thisIsTheBestPasswordOfAllTime!");
    }

    @Test
    void should_fail_parsing_secret_mount() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        GraviteeConfigurationSecretResolverDispatcher cut = newDispatcher(pluginManager, env);

        assertThatCode(() -> cut.toSecretMount("secret://foooo/test:password")).isInstanceOf(SecretProviderNotFoundException.class);

        // the test secret provider expect the path to always be 'test'
        assertThatCode(() -> cut.toSecretMount("secret://test/this_is_not_a_valid_path_for_our_fake_impl:password"))
            .isInstanceOf(SecretManagerConfigurationException.class);
    }
}
