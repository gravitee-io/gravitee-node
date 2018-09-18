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
package io.gravitee.node.notifier.plugin;

import io.gravitee.node.notifier.NotifierService;
import io.gravitee.notifier.api.Notifier;
import io.gravitee.plugin.core.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotifierPluginHandler implements PluginHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierPluginHandler.class);

    @Autowired
    private Environment environment;
    @Autowired
    private PluginContextFactory pluginContextFactory;
    @Autowired
    private PluginClassLoaderFactory pluginClassLoaderFactory;
    @Autowired
    private NotifierService notifierService;

    @Override
    public boolean canHandle(Plugin plugin) {
        return PluginType.NOTIFIER == plugin.type();
    }

    @Override
    public void handle(Plugin plugin) {
        LOGGER.info("Register a new notifier: {} [{}]", plugin.id(), plugin.clazz());
        boolean enabled = isEnabled(plugin);
        if (enabled) {
            try {
                pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
                final ApplicationContext context = pluginContextFactory.create(plugin);
                final Notifier notifier = context.getBean(Notifier.class);
                notifierService.register(notifier);
            } catch (Exception iae) {
                LOGGER.error("Unexpected error while create notifier instance", iae);
                // Be sure that the context does not exist anymore.
                pluginContextFactory.remove(plugin);
            }
        } else {
            LOGGER.warn("Plugin {} is disabled. Please have a look to your configuration to re-enable it", plugin.id());
        }
    }

    private boolean isEnabled(Plugin notifierPlugin) {
        boolean enabled = environment.getProperty(notifierPlugin.id() + ".enabled", Boolean.class, true);
        LOGGER.debug("Plugin {} configuration: {}", notifierPlugin.id(), enabled);
        return enabled;
    }
}
