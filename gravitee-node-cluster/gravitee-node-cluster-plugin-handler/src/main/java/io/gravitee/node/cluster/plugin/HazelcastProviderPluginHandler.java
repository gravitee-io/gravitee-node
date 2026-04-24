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

import io.gravitee.node.api.cluster.DistributedMapProvider;
import io.gravitee.plugin.core.api.AbstractPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Handles plugins of type {@code hazelcast-provider}. The plugin owns the embedded Hazelcast
 * instance and exposes it as a {@link DistributedMapProvider}. Both are registered as singletons
 * in the main application context so that siblings (e.g. the {@code cluster-hazelcast} plugin's
 * {@code HazelcastClusterManager}, or APIM's rate-limit repository) can autowire them.
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so that downstream cluster plugins finding
 * {@code HazelcastInstance} in the main context don't race the registration.</p>
 */
@CustomLog
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HazelcastProviderPluginHandler extends AbstractPluginHandler {

    private static final String PLUGIN_TYPE = "hazelcast-provider";

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    private PluginClassLoaderFactory<Plugin> pluginClassLoaderFactory;

    @Autowired
    private ApplicationContext applicationContext;

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
            ApplicationContext context = pluginContextFactory.create(plugin);
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) (
                (ConfigurableApplicationContext) applicationContext
            ).getBeanFactory();

            // Promote the provider to the main context so siblings can autowire it.
            DistributedMapProvider provider = context.getBean(DistributedMapProvider.class);
            beanFactory.registerSingleton(DistributedMapProvider.class.getName(), provider);

            // Also promote the underlying HazelcastInstance so the cluster-hazelcast plugin can
            // consume it without creating a second instance.
            Object hazelcastInstance = context.getBean("clusterHazelcastInstance");
            beanFactory.registerSingleton("clusterHazelcastInstance", hazelcastInstance);

            log.info("Hazelcast provider plugin '{}' installed.", plugin.id());
        } catch (Exception e) {
            log.error("Unexpected error while registering hazelcast provider {}", plugin.id(), e);
            pluginContextFactory.remove(plugin);
        }
    }

    @Override
    protected ClassLoader getClassLoader(final Plugin plugin) {
        return pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
    }
}
