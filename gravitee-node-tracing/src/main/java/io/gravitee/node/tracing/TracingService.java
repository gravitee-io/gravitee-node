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
package io.gravitee.node.tracing;

import io.gravitee.node.api.tracing.Tracer;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingService implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(TracingService.class);

    private final List<TracerListener> listeners = new ArrayList<>();

    @Autowired
    private PluginClassLoaderFactory pluginClassLoaderFactory;

    @Autowired
    private Environment environment;

    @Autowired
    private PluginContextFactory pluginContextFactory;

    public void register(Plugin plugin) {
            String tracerType = environment.getProperty("services.tracing.type");

        if (tracerType != null && plugin.id().contains(tracerType)) {
            try {
                PluginClassLoader classLoader = pluginClassLoaderFactory.getOrCreateClassLoader(plugin);
                Class<?> clazz = classLoader.loadClass(plugin.clazz());

                ApplicationContext context = this.pluginContextFactory.create(plugin);
                Tracer tracer = (Tracer) context.getBean(clazz);

                tracer.start();

                for (TracerListener listener : listeners) {
                    listener.onRegister(tracer);
                }
            } catch (Exception ex) {
                logger.error("Unable to create an instance of Tracer: {}", plugin.id(), ex);
            }
        } else {
            logger.warn("Tracing support is enabled for tracer name[{}]. Skipping the {} tracer installation",
                    tracerType, plugin.id());
        }
    }

    public void addTracerListener(TracerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String tracerType = environment.getProperty("services.tracing.type");
        logger.info("Tracing support is enabled with tracer: name[{}]", tracerType);
    }

    public interface TracerListener {
        void onRegister(Tracer tracer);
    }
}
