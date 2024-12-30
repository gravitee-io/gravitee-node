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
package io.gravitee.node.secrets.plugins.internal;

import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.spring.SecretProviderPluginConfiguration;
import io.gravitee.plugin.core.api.AbstractSimplePluginHandler;
import io.gravitee.plugin.core.api.BootPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProvider;
import java.io.IOException;
import java.net.URLClassLoader;
import org.springframework.context.annotation.Import;

/**
 * @author GraviteeSource Team
 */
@Import(SecretProviderPluginConfiguration.class)
public class SecretProviderPluginHandler extends AbstractSimplePluginHandler<SecretProviderPlugin<?, ?>> implements BootPluginHandler {

    private final DefaultSecretProviderPluginManager secretProviderPluginManager;

    public SecretProviderPluginHandler(DefaultSecretProviderPluginManager secretProviderPluginManager) {
        this.secretProviderPluginManager = secretProviderPluginManager;
    }

    @Override
    public boolean canHandle(final Plugin plugin) {
        return SecretProvider.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return SecretProvider.PLUGIN_TYPE;
    }

    @Override
    @SuppressWarnings({ "rawtypes, unchecked", "java:S3740" })
    protected SecretProviderPlugin<?, ?> create(final Plugin plugin, final Class<?> pluginClass) {
        Class<? extends SecretManagerConfiguration> configurationClass = new SecretManagerConfigurationClassFinder()
            .lookupFirst(pluginClass);

        return new DefaultSecretProviderPlugin(plugin, pluginClass, configurationClass);
    }

    @Override
    protected void register(SecretProviderPlugin<?, ?> secretProviderPlugin) {
        secretProviderPluginManager.register(secretProviderPlugin);

        // Once registered, the classloader should be released
        final ClassLoader classLoader = secretProviderPlugin.secretProviderFactory().getClassLoader();
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            try {
                urlClassLoader.close();
            } catch (IOException e) {
                logger.error("Unexpected exception while trying to release the policy classloader");
            }
        }
    }

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) {
        return new URLClassLoader(plugin.dependencies(), this.getClass().getClassLoader());
    }
}
