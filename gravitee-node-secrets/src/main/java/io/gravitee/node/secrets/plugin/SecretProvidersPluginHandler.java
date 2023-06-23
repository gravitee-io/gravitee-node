/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.secrets.plugin;

import io.gravitee.node.secrets.SecretProviderService;
import io.gravitee.plugin.core.api.AbstractSpringPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretProvidersPluginHandler extends AbstractSpringPluginHandler<SecretProviderService> {

    @Autowired
    private PluginClassLoaderFactory pluginClassLoaderFactory;

    @Autowired
    private SecretProvidersManager secretProvidersManager;

    @Override
    public boolean canHandle(Plugin plugin) {
        return type().equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return "secret-provider";
    }

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) throws Exception {
        return pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());
    }

    @Override
    protected void register(SecretProviderService plugin) {
        secretProvidersManager.register(plugin);
    }
}
