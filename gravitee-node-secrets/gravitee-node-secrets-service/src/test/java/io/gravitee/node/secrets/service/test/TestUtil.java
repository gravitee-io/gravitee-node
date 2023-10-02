package io.gravitee.node.secrets.service.test;

import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderClassLoaderFactory;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPlugin;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TestUtil {

    public static MockEnvironment newEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("secrets.test.enabled", "true");
        return env;
    }

    public static GraviteeConfigurationSecretResolverDispatcher newDispatcher(
        DefaultSecretProviderPluginManager pluginManager,
        MockEnvironment env
    ) {
        GraviteeConfigurationSecretResolverDispatcher cut = new GraviteeConfigurationSecretResolverDispatcher(pluginManager, env);
        pluginManager.register(
            new DefaultSecretProviderPlugin<>(
                new TestSecretProviderPlugin(true),
                TestSecretProviderFactory.class,
                TestSecretProviderConfiguration.class
            )
        );
        return cut;
    }

    public static DefaultSecretProviderPluginManager newPluginManager() {
        return new DefaultSecretProviderPluginManager(new DefaultSecretProviderClassLoaderFactory());
    }
}
