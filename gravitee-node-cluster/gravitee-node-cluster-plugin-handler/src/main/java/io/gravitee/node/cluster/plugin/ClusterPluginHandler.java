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
import io.gravitee.node.api.cluster.DistributedMapProvider;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.cluster.spring.NodeClusterPluginConfiguration;
import io.gravitee.plugin.core.api.AbstractPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import lombok.CustomLog;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Import(NodeClusterPluginConfiguration.class)
public class ClusterPluginHandler extends AbstractPluginHandler {

    private static final String PLUGIN_TYPE = "cluster";
    private static final String HAZELCAST_PLUGIN_ID = "cluster-hazelcast";

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
        final boolean asClusterManager = isConfiguredClusterPlugin(plugin.id());
        final boolean asDistributedMapProvider = !asClusterManager && HAZELCAST_PLUGIN_ID.equals(plugin.id()) && isHazelcastEnabled();

        if (!asClusterManager && !asDistributedMapProvider) {
            log.debug("Cluster manager plugin '{}' is not the type configured and won't be installed.", plugin.id());
            return;
        }

        try {
            ApplicationContext context = pluginContextFactory.create(plugin);
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) (
                (ConfigurableApplicationContext) applicationContext
            ).getBeanFactory();

            if (asClusterManager) {
                ClusterManager clusterManager = (ClusterManager) context.getBean(pluginClass);
                beanFactory.registerSingleton(ClusterManager.class.getName(), clusterManager);
                log.info("Cluster manager plugin '{}' installed.", plugin.id());
            } else {
                log.info("Cluster plugin '{}' loaded as distributed-map provider (cluster.type={}).", plugin.id(), configuredClusterType());
            }

            // Expose DistributedMapProvider from any cluster plugin that declares one (e.g. Hazelcast).
            // This decouples distributed-map availability from cluster.type: operators can run
            // cluster.type=standalone + hazelcast.enabled=true to get HZ-backed distributed maps
            // without APIM cluster sync going through HZ.
            try {
                DistributedMapProvider provider = context.getBean(DistributedMapProvider.class);
                beanFactory.registerSingleton(DistributedMapProvider.class.getName(), provider);
                log.info("DistributedMapProvider registered from plugin '{}'.", plugin.id());
            } catch (NoSuchBeanDefinitionException ignored) {
                // Plugin doesn't offer a DistributedMapProvider (e.g. standalone). Normal.
            }
        } catch (NoSuchBeanDefinitionException nsbde) {
            logger.info("No ClusterManager instance has been detected. Skipping.");
        } catch (Exception e) {
            logger.error("Unexpected error while registering cluster manager {}", plugin.id(), e);
            // Be sure that the context does not exist anymore.
            pluginContextFactory.remove(plugin);
        }
    }

    private boolean isConfiguredClusterPlugin(final String clusterPluginId) {
        final String configuredClusterType = configuredClusterType();
        final String prefixedType = PLUGIN_TYPE + "-" + configuredClusterType;
        return configuredClusterType.equals(clusterPluginId) || prefixedType.equals(clusterPluginId);
    }

    private String configuredClusterType() {
        return configuration.getProperty("cluster.type", "standalone");
    }

    private boolean isHazelcastEnabled() {
        return Boolean.parseBoolean(configuration.getProperty("hazelcast.enabled", "false"));
    }

    @Override
    protected ClassLoader getClassLoader(final Plugin plugin) {
        return pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
    }
}
