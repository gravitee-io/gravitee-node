/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.secrets.plugins.internal;

import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author GraviteeSource Team
 */
public class DefaultSecretProviderPlugin<F extends SecretProviderFactory<C>, C extends SecretManagerConfiguration>
    implements SecretProviderPlugin<F, C> {

    private final Plugin plugin;
    private final Class<F> secretProviderClass;
    private final Class<C> secretProviderConfigurationClass;

    public DefaultSecretProviderPlugin(
        final Plugin plugin,
        final Class<F> secretProviderFactoryClass,
        final Class<C> secretProviderConfigurationClass
    ) {
        this.plugin = plugin;
        this.secretProviderClass = secretProviderFactoryClass;
        this.secretProviderConfigurationClass = secretProviderConfigurationClass;
    }

    @Override
    public Class<F> secretProviderFactory() {
        return secretProviderClass;
    }

    @Override
    public String clazz() {
        return plugin.clazz();
    }

    @Override
    public URL[] dependencies() {
        return plugin.dependencies();
    }

    @Override
    public String id() {
        return plugin.id();
    }

    @Override
    public PluginManifest manifest() {
        return plugin.manifest();
    }

    @Override
    public Path path() {
        return plugin.path();
    }

    @Override
    public Class<C> configuration() {
        return secretProviderConfigurationClass;
    }

    @Override
    public boolean deployed() {
        return plugin.deployed();
    }
}
