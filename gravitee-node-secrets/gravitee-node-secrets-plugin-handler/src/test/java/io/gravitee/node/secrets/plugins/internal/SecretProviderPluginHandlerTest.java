package io.gravitee.node.secrets.plugins.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.internal.test.TestSecretProviderFactory;
import io.gravitee.node.secrets.plugins.internal.test.TestSecretProviderPlugin;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretProviderPluginHandlerTest {

    private final DefaultSecretProviderPluginManager pluginManager = new DefaultSecretProviderPluginManager(
        new DefaultSecretProviderClassLoaderFactory()
    );
    final SecretProviderPluginHandler cut = new SecretProviderPluginHandler(pluginManager);

    @Test
    void should_be_able_to_handle_and_have_correct_type() {
        assertThat(cut.canHandle(new TestSecretProviderPlugin(true))).isTrue();
        assertThat(cut.type()).isEqualTo(SecretProvider.PLUGIN_TYPE);
    }

    @Test
    void should_handle_deployed() {
        cut.handle(new TestSecretProviderPlugin(true), TestSecretProviderFactory.class);
        SecretProviderPlugin<?, ?> plugin = pluginManager.get("test");
        assertThat(plugin).isNotNull();
        assertThat(plugin.secretProviderFactory()).isEqualTo(TestSecretProviderFactory.class);
        assertThat(plugin.manifest()).isNotNull();
        assertThat(plugin.path()).isNotNull();
        assertThat(cut.getClassLoader(plugin)).isNotNull();
    }

    @Test
    void should_handle_not_deployed() {
        cut.handle(new TestSecretProviderPlugin(false), TestSecretProviderFactory.class);
        assertThat(pluginManager.get("test")).isNull();
        assertThat(pluginManager.get("test", true)).isNotNull();
    }
}
