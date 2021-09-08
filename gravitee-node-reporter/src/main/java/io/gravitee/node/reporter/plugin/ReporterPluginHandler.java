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
package io.gravitee.node.reporter.plugin;

import io.gravitee.node.reporter.ReporterManager;
import io.gravitee.plugin.core.api.AbstractSpringPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.reporter.api.Reporter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReporterPluginHandler
  extends AbstractSpringPluginHandler<Reporter> {

  private static final String PLUGIN_TYPE = "reporter";

  @Autowired
  private PluginClassLoaderFactory pluginClassLoaderFactory;

  @Autowired
  private ReporterManager reporterManager;

  @Override
  public boolean canHandle(Plugin plugin) {
    return PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
  }

  @Override
  protected String type() {
    return "reporters";
  }

  @Override
  protected ClassLoader getClassLoader(Plugin plugin) throws Exception {
    return pluginClassLoaderFactory.getOrCreateClassLoader(
      plugin,
      this.getClass().getClassLoader()
    );
  }

  @Override
  protected void register(Reporter plugin) {
    reporterManager.register(plugin);
  }
}
