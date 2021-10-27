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
package io.gravitee.node.container.spring;

import io.gravitee.node.api.configuration.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

public class SpringEnvironmentConfiguration implements Configuration {

  private final Environment environment;

  public SpringEnvironmentConfiguration(Environment environment) {
    this.environment = environment;
  }

  @Override
  public boolean containsProperty(String key) {
    return environment.containsProperty(key);
  }

  @Override
  @Nullable
  public String getProperty(String key) {
    return environment.getProperty(key);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    return environment.getProperty(key, defaultValue);
  }

  @Override
  @Nullable
  public <T> T getProperty(String key, Class<T> targetType) {
    return environment.getProperty(key, targetType);
  }

  @Override
  public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
    return environment.getProperty(key, targetType, defaultValue);
  }
}
