package io.gravitee.node.secrets;

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

    void setOnNewPluginCallback(Consumer<String> callback);
}
