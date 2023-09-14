package io.gravitee.node.secrets.plugins;

import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import java.util.function.Consumer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProviderPluginManager extends ConfigurablePluginManager<SecretProviderPlugin<?, ?>> {
    default <T extends SecretProviderFactory> T getFactoryById(final String pluginId) {
        return getFactoryById(pluginId, false);
    }

    <T extends SecretProviderFactory> T getFactoryById(final String pluginId, boolean includeNotDeployed);

    /**
     * to set a callback that will be called once the plugin is loaded. This meant to have a spring bean know that the plugin has just been loaded.
     *
     * @param callback a consumer of the plugin id
     */
    void setOnNewPluginCallback(Consumer<String> callback);
}
