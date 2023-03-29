/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.cluster.plugin;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.core.api.AbstractPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ClusterPluginHandler extends AbstractPluginHandler {

    private static final String PLUGIN_TYPE = "cluster";

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    private PluginClassLoaderFactory<Plugin> pluginClassLoaderFactory;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Configuration configuration;

    @Override
    public boolean canHandle(Plugin plugin) {
        return plugin.type().equalsIgnoreCase(type());
    }

    @Override
    protected String type() {
        return PLUGIN_TYPE;
    }

    @Override
    protected void handle(final Plugin plugin, final Class<?> pluginClass) {
        try {
            // Check if the current cluster plugin is the one configured
            if (isConfiguredClusterPlugin(plugin.id())) {
                // Create spring application context
                ApplicationContext context = pluginContextFactory.create(plugin);

                // Retrieve actual ClusterManager bean and register it as Singleton
                ClusterManager clusterManager = (ClusterManager) context.getBean(pluginClass);
                DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) (
                    (ConfigurableApplicationContext) applicationContext
                ).getBeanFactory();
                beanFactory.registerSingleton(ClusterManager.class.getName(), clusterManager);
            }
        } catch (Exception e) {
            logger.error("Unexpected error while registering cluster manager {}", plugin.id(), e);
            // Be sure that the context does not exist anymore.
            pluginContextFactory.remove(plugin);
        }
    }

    private boolean isConfiguredClusterPlugin(final String clusterPluginId) {
        final String configuredClusterType = configuration.getProperty("cluster.type", "standalone");
        final String prefixedType = PLUGIN_TYPE + "-" + configuredClusterType;
        return configuredClusterType.equals(clusterPluginId) || prefixedType.equals(clusterPluginId);
    }

    @Override
    protected ClassLoader getClassLoader(final Plugin plugin) {
        return pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
    }
}
