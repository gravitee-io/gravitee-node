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
package io.gravitee.node.plugins.service.handler;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.plugins.service.ServiceManager;
import io.gravitee.plugin.core.api.AbstractSpringPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServicePluginHandler extends AbstractSpringPluginHandler<AbstractService> {

    private static final String PLUGIN_TYPE = "service";

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private PluginClassLoaderFactory pluginClassLoaderFactory;

    @Override
    public boolean canHandle(Plugin plugin) {
        return PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return "services";
    }

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) throws Exception {
        return pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
    }

    @Override
    protected void register(AbstractService plugin) {
        serviceManager.register(plugin);
    }
}
