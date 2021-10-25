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
package io.gravitee.node.tracing.plugin;

import io.gravitee.node.tracing.TracingService;
import io.gravitee.plugin.core.api.AbstractPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingPluginHandler extends AbstractPluginHandler {

    @Autowired
    private PluginClassLoaderFactory<Plugin> pluginClassLoaderFactory;

    @Autowired
    private TracingService tracingService;

    @Autowired
    private Environment environment;

    @Override
    public boolean canHandle(Plugin plugin) {
        return (
            environment.getProperty("services.tracing.enabled", Boolean.class, false) &&
            TracerPlugin.PLUGIN_TYPE.equalsIgnoreCase(plugin.type())
        );
    }

    @Override
    protected String type() {
        return "tracers";
    }

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) throws Exception {
        return pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
    }

    @Override
    protected void handle(Plugin plugin, Class<?> aClass) {
        tracingService.register(plugin);
    }
}
