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

import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.message.MessageProducer;
import io.gravitee.node.cluster.ClusterManagerDelegate;
import io.gravitee.node.cluster.MessageProducerDelegate;
import io.gravitee.node.cluster.cache.CacheManagerDelegate;
import io.gravitee.plugin.core.api.*;
import io.gravitee.plugin.core.internal.AnnotationBasedPluginContextConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClusterPluginHandler implements PluginHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPluginHandler.class);

    @Autowired
    private Configuration configuration;

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    private PluginClassLoaderFactory<Plugin> pluginClassLoaderFactory;

    @Autowired
    private ClusterManager clusterManager;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MessageProducer messageProducer;

    @Override
    public boolean canHandle(final Plugin plugin) {
        return ClusterPlugin.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            final String type = configuration.getProperty("configuration.type", "standalone");
            final String prefixedType = "cluster-" + type;

            if (type.equals(plugin.id()) || prefixedType.equals(plugin.id())) {
                ClassLoader classloader = pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
                final Class<?> clusterClass = classloader.loadClass(plugin.clazz());

                Assert.isAssignable(ClusterManager.class, clusterClass);

                loadClusterManager(plugin);
            }
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create cluster manager", iae);
        }
    }

    private boolean loadClusterManager(Plugin plugin) {
        LOGGER.info("Cluster manager [{}] loaded by {}", plugin.id(), plugin.clazz());

        try {
            ApplicationContext clusterApplicationContext = pluginContextFactory.create(new AnnotationBasedPluginContextConfigurer(plugin) {
                @Override
                public void registerBeans() {
                    // do nothing, but do it well
                }
            });

            ((ClusterManagerDelegate) clusterManager).setTarget(clusterApplicationContext.getBean(ClusterManager.class));
            ((CacheManagerDelegate) cacheManager).setTarget(clusterApplicationContext.getBean(CacheManager.class));
            ((MessageProducerDelegate) messageProducer).setTarget(clusterApplicationContext.getBean(MessageProducer.class));
            return true;
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while creating context for cluster manager", iae);
            pluginContextFactory.remove(plugin);

            return false;
        }
    }
}
