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
package io.gravitee.node.certificates;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactory;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeyStoreLoaderManager
  extends AbstractService<KeyStoreLoaderManager> {

  private final Set<KeyStoreLoaderFactory> loaderFactories;
  private final Set<KeyStoreLoader> loaders;

  public KeyStoreLoaderManager() {
    this.loaderFactories = new HashSet<>();
    this.loaders = new HashSet<>();

    // Automatically register the file keystore nd self-signed loader factories.
    this.registerFactory(new FileKeyStoreLoaderFactory());
    this.registerFactory(new SelfSignedKeyStoreLoaderFactory());
  }

  @Override
  public KeyStoreLoaderManager preStop() throws Exception {
    loaders.forEach(KeyStoreLoader::stop);
    return this;
  }

  public void registerFactory(KeyStoreLoaderFactory keyStoreLoaderFactory) {
    loaderFactories.add(keyStoreLoaderFactory);
  }

  public Set<KeyStoreLoaderFactory> getLoaderFactories() {
    return loaderFactories;
  }

  public KeyStoreLoader create(KeyStoreLoaderOptions options) {
    return getLoaderFactories()
      .stream()
      .filter(keyStoreLoaderFactory -> keyStoreLoaderFactory.canHandle(options))
      .findFirst()
      .map(keyStoreLoaderFactory -> keyStoreLoaderFactory.create(options))
      .orElse(null);
  }
}
