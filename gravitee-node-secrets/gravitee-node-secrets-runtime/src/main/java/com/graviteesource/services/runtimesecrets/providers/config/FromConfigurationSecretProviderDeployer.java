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
package com.graviteesource.services.runtimesecrets.providers.config;

import static com.graviteesource.services.runtimesecrets.config.Config.CONFIG_PREFIX;

import com.graviteesource.services.runtimesecrets.providers.SecretProviderRegistry;
import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.node.api.secrets.errors.SecretManagerConfigurationException;
import io.gravitee.node.api.secrets.errors.SecretProviderNotFoundException;
import io.gravitee.node.api.secrets.runtime.providers.SecretProviderDeployer;
import io.gravitee.node.api.secrets.util.ConfigHelper;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@RequiredArgsConstructor
@Slf4j
public class FromConfigurationSecretProviderDeployer implements SecretProviderDeployer {

    private final ConfigurableEnvironment environment;
    private final SecretProviderRegistry registry;
    private final SecretProviderPluginManager secretProviderPluginManager;

    public void init() {
        log.info("loading runtime secret providers from configuration");
        Map<String, Object> allProperties = EnvironmentUtils.getAllProperties(environment);
        Map<String, Object> apiSecrets = ConfigHelper.removePrefix(allProperties, CONFIG_PREFIX);
        int i = 0;
        String provider = provider(i);
        while (apiSecrets.containsKey(provider + ".plugin")) {
            Map<String, Object> providerConfig = ConfigHelper.removePrefix(apiSecrets, provider);
            if (!ConfigHelper.getProperty(providerConfig, "configuration.enabled", Boolean.class, true)) {
                return;
            }
            String plugin = ConfigHelper.getProperty(providerConfig, "plugin", String.class);
            String id = ConfigHelper.getProperty(providerConfig, "id", String.class, plugin);
            int e = 0;
            String environment = environment(e);
            while (providerConfig.containsKey(environment)) {
                String envId = providerConfig.get(environment).toString();
                deploy(plugin, ConfigHelper.removePrefix(providerConfig, "configuration"), id, envId);
                environment = environment(++e);
            }
            // no env
            if (e == 0) {
                deploy(plugin, ConfigHelper.removePrefix(providerConfig, "configuration"), id, null);
            }
            provider = provider(++i);
        }
    }

    @Override
    public void deploy(String pluginId, Map<String, Object> configurationProperties, String providerId, String envId) {
        try {
            log.info("Deploying secret provider [{}] of type [{}] for environment [{}]...", providerId, pluginId, formatEnv(envId));
            final SecretProviderPlugin<?, ?> secretProviderPlugin = secretProviderPluginManager.get(pluginId);
            final Class<? extends SecretManagerConfiguration> configurationClass = secretProviderPlugin.configuration();
            final SecretProviderFactory<SecretManagerConfiguration> factory = secretProviderPluginManager.getFactoryById(pluginId);
            if (configurationClass != null && factory != null) {
                // read the config using the plugin class loader
                SecretManagerConfiguration config;
                Class<?> configurationClass1 = factory.getClass().getClassLoader().loadClass(configurationClass.getName());
                try {
                    @SuppressWarnings("unchecked")
                    Constructor<SecretManagerConfiguration> constructor = (Constructor<SecretManagerConfiguration>) configurationClass1.getDeclaredConstructor(
                        Map.class
                    );
                    config = constructor.newInstance(configurationProperties);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new SecretManagerConfigurationException(
                        "Could not create configuration class for secret manager: %s".formatted(providerId),
                        e
                    );
                }
                log.info("Secret provider [{}] of type [{}] for environment [{}]: DEPLOYED", providerId, pluginId, formatEnv(envId));
                this.registry.register(providerId, factory.create(config).start(), envId);
            } else {
                log.info("Secret provider [{}] of type [{}] for environment [{}]: FAILED", providerId, pluginId, formatEnv(envId));
                throw new SecretProviderNotFoundException("Cannot find secret provider [%s] plugin".formatted(pluginId));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot load plugin %s properly: ".formatted(pluginId));
        }
    }

    private static String formatEnv(String envId) {
        return envId == null ? "*" : envId;
    }

    private static String provider(int i) {
        return "providers[%d]".formatted(i);
    }

    private static String environment(int e) {
        return "environments[%d]".formatted(e);
    }
}
