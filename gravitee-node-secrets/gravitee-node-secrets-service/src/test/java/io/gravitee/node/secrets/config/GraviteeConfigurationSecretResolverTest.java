package io.gravitee.node.secrets.config;

import static io.gravitee.node.secrets.config.test.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import io.gravitee.secrets.api.core.Secret;
import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.errors.SecretProviderNotFoundException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteeConfigurationSecretResolverTest {

    @Test
    void should_create_secret_provider_that_can_handle_resolution() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        GraviteeConfigurationSecretResolver cut = newDispatcher(pluginManager, env);

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
        GraviteeConfigurationSecretResolver cut = newDispatcher(pluginManager, env);
        SecretURL secretURL = cut.asSecretURL("secret://test/test:password");
        assertThat(secretURL.key()).isEqualTo("password");
        assertThat(secretURL.provider()).isEqualTo("test");

        Secret secret = cut.resolveKey(secretURL).blockingGet();
        assertThat(secret).isNotNull();
        assertThat(secret.asString()).isEqualTo("noOneWillFindMyPasswordHahahaha");
        // secret was cached
        assertThat(cut.secrets().get(secretURL.path())).isNotNull();

        SecretMap secretMap = cut.resolve(secretURL).blockingGet();
        assertThat(secretMap).isNotNull();
        assertThat(secretMap.getSecret(secretURL))
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
        GraviteeConfigurationSecretResolver cut = newDispatcher(pluginManager, env);
        SecretURL secretURL = cut.asSecretURL("secret://test/test:password");

        Secret secret = cut.watchKey(secretURL).blockingFirst(); // don't about the rest
        assertThat(secret.asString()).isEqualTo("thisIsTheBestPasswordOfAllTime!");
        // secret was NOT cached
        assertThat(cut.secrets().get(secretURL.path())).isNull();

        SecretMap secretMap = cut.watch(secretURL).blockingFirst();
        assertThat(secretMap).isNotNull();
        assertThat(secretMap.getSecret(secretURL))
            .isPresent()
            .get()
            .extracting(Secret::asString)
            .isEqualTo("thisIsTheBestPasswordOfAllTime!");
    }

    @Test
    void should_create_secret_provider_and_watch_filtered() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        env.setProperty("secrets.test.secrets.password", "thisIsTheBestPasswordOfAllTime!");
        GraviteeConfigurationSecretResolver cut = newDispatcher(pluginManager, env);
        SecretURL secretURL = cut.asSecretURL("secret://test/test:password");

        // make sure we have two different maps (Flowable always returns the same)
        SecretMap first = cut.watch(secretURL).blockingFirst();
        SecretMap last = cut.watch(secretURL).blockingLast();
        assertThat(first).isNotEqualTo(last).isNotNull();
        assertThat(first.getSecret(SecretURL.from("secret://foo/bar:created_flag"))).isPresent();
        assertThat(first.getSecret(SecretURL.from("secret://foo/bar:updated_flag"))).isNotPresent();
        assertThat(last.getSecret(SecretURL.from("secret://foo/bar:created_flag"))).isNotPresent();
        assertThat(last.getSecret(SecretURL.from("secret://foo/bar:updated_flag"))).isPresent();

        first = cut.watch(secretURL, SecretEvent.Type.UPDATED).blockingFirst();
        last = cut.watch(secretURL, SecretEvent.Type.UPDATED).blockingLast();
        assertThat(first).isEqualTo(last).isNotNull();
        assertThat(first.getSecret(SecretURL.from("secret://foo/bar:created_flag"))).isNotPresent();
        assertThat(first.getSecret(SecretURL.from("secret://foo/bar:updated_flag"))).isPresent();

        first = cut.watch(secretURL, SecretEvent.Type.CREATED).blockingFirst();
        last = cut.watch(secretURL, SecretEvent.Type.CREATED).blockingLast();
        assertThat(first).isEqualTo(last).isNotNull();
        assertThat(first.getSecret(SecretURL.from("secret://foo/bar:created_flag"))).isPresent();
        assertThat(first.getSecret(SecretURL.from("secret://foo/bar:updated_flag"))).isNotPresent();

        Iterable<SecretMap> all = cut.watch(secretURL, SecretEvent.Type.DELETED).blockingIterable();
        assertThat(all).isEmpty();
    }

    @Test
    void should_fail_validating_url() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        GraviteeConfigurationSecretResolver cut = newDispatcher(pluginManager, env);

        assertThatCode(() -> cut.asSecretURL("secret://foooo/test:password")).isInstanceOf(SecretProviderNotFoundException.class);
    }
}
