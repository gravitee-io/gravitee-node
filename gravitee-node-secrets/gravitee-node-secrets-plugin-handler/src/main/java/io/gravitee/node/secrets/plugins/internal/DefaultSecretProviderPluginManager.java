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

import io.gravitee.node.secrets.plugins.SecretProviderClassLoaderFactory;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.plugin.core.api.AbstractConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@Slf4j
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DefaultSecretProviderPluginManager
    extends AbstractConfigurablePluginManager<SecretProviderPlugin<?, ?>>
    implements SecretProviderPluginManager {

    private final SecretProviderClassLoaderFactory classLoaderFactory;
    private final Map<String, SecretProviderFactory> factories = new HashMap<>();

    private final Map<String, SecretProviderFactory> notDeployedPluginFactories = new HashMap<>();
    private Consumer<String> onNewPluginCallback;

    public DefaultSecretProviderPluginManager(final SecretProviderClassLoaderFactory classLoaderFactory) {
        this.classLoaderFactory = classLoaderFactory;
    }

    @Override
    public void setOnNewPluginCallback(Consumer<String> callback) {
        this.onNewPluginCallback = callback;
    }

    @Override
    public void register(final SecretProviderPlugin<?, ?> plugin) {
        super.register(plugin);

        PluginClassLoader pluginClassLoader = classLoaderFactory.getOrCreateClassLoader(plugin);
        try {
            final Class<SecretProviderFactory> secretProviderFactoryClass = (Class<SecretProviderFactory>) pluginClassLoader.loadClass(
                plugin.clazz()
            );
            SecretProviderFactory factory = createFactory(secretProviderFactoryClass);
            if (plugin.deployed()) {
                factories.put(plugin.id(), factory);
            } else {
                notDeployedPluginFactories.put(plugin.id(), factory);
            }
            if (onNewPluginCallback != null) {
                onNewPluginCallback.accept(plugin.id());
            }
        } catch (Exception ex) {
            log.error("Unexpected error while loading secret provider plugin: {}", plugin.clazz(), ex);
        }
    }

    private SecretProviderFactory createFactory(final Class<SecretProviderFactory> factoryClass)
        throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Constructor<SecretProviderFactory> emptyConstructor = factoryClass.getDeclaredConstructor();
        return emptyConstructor.newInstance();
    }

    @Override
    public SecretProviderFactory getFactoryById(String secretProviderPluginId, boolean includeNotDeployed) {
        SecretProviderFactory factory = factories.get(secretProviderPluginId);
        if (factory == null && includeNotDeployed) {
            return notDeployedPluginFactories.get(secretProviderPluginId);
        }
        return factory;
    }
}
