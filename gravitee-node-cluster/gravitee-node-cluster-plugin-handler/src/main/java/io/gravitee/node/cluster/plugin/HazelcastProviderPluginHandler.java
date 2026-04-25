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
import io.gravitee.plugin.core.api.AbstractPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Handles plugins of type {@code hazelcast-provider}. This plugin owns the embedded
 * {@link com.hazelcast.core.HazelcastInstance} and exposes three roles from a single classloader:
 *
 * <ul>
 *   <li>Always: {@link DistributedMapProvider} — registered in the main application context so any
 *       consumer (e.g. APIM's rate-limit repository) can autowire it.</li>
 *   <li>When {@code cluster.type=hazelcast}: the Hazelcast-backed {@link ClusterManager} — also
 *       registered in the main application context, replacing the role previously filled by the
 *       (now-retired) {@code cluster-hazelcast} plugin.</li>
 * </ul>
 *
 * <p>Keeping ownership of the Hazelcast instance + cluster-manager in the same plugin is required
 * because plugin classloaders are isolated: a {@link HazelcastInstance} created in one plugin's
 * classloader is a different {@code Class} object from the one in another plugin's, so Spring
 * autowiring across plugin boundaries fails on types drawn from bundled third-party libraries.</p>
 */
@CustomLog
public class HazelcastProviderPluginHandler extends AbstractPluginHandler {

    private static final String PLUGIN_TYPE = "hazelcast-provider";
    private static final String HAZELCAST_CLUSTER_MANAGER_BEAN = "hazelcastClusterManager";

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
            ApplicationContext context = pluginContextFactory.create(plugin);
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) (
                (ConfigurableApplicationContext) applicationContext
            ).getBeanFactory();

            // Always expose DistributedMapProvider. The provider holds HazelcastInstance through a
            // Spring @Lazy proxy, so fetching the bean here does NOT boot Hazelcast — HZ is deferred
            // until the first get(...) call. That lets operators run cluster.type=standalone +
            // ratelimit.type!=hazelcast with zero HZ overhead, even though the provider plugin is
            // always bundled in the default distribution.
            DistributedMapProvider provider = context.getBean(DistributedMapProvider.class);
            beanFactory.registerSingleton(DistributedMapProvider.class.getName(), provider);

            // Register HazelcastClusterManager as the active ClusterManager only when cluster.type=hazelcast.
            // Fetching this bean DOES boot Hazelcast (the cluster manager needs the live instance at startup
            // for membership, topics, queues) — which is the correct behaviour under cluster.type=hazelcast.
            if (isHazelcastClusterConfigured()) {
                ClusterManager clusterManager = (ClusterManager) context.getBean(HAZELCAST_CLUSTER_MANAGER_BEAN);
                beanFactory.registerSingleton(ClusterManager.class.getName(), clusterManager);
                log.info("HazelcastClusterManager registered (cluster.type=hazelcast).");
            }

            log.info("Hazelcast provider plugin '{}' installed.", plugin.id());
        } catch (Exception e) {
            log.error("Unexpected error while registering hazelcast provider {}", plugin.id(), e);
            pluginContextFactory.remove(plugin);
        }
    }

    private boolean isHazelcastClusterConfigured() {
        return "hazelcast".equalsIgnoreCase(configuration.getProperty("cluster.type", "standalone"));
    }

    @Override
    protected ClassLoader getClassLoader(final Plugin plugin) {
        return pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
    }
}
