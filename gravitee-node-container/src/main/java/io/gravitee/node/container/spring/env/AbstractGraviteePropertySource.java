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
package io.gravitee.node.container.spring.env;

import io.gravitee.node.kubernetes.propertyresolver.CloudScheme;
import io.gravitee.node.kubernetes.propertyresolver.PropertyResolver;
import io.gravitee.node.kubernetes.propertyresolver.PropertyResolverFactoriesLoader;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGraviteePropertySource
  extends PropertySource<Map<String, Object>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    AbstractGraviteePropertySource.class
  );
  private final PropertyResolverFactoriesLoader propertyResolverLoader;

  protected AbstractGraviteePropertySource(
    String name,
    Map<String, Object> source,
    ApplicationContext applicationContext
  ) {
    super(name, source);
    this.propertyResolverLoader =
      applicationContext.getBean(PropertyResolverFactoriesLoader.class);
  }

  @Override
  public Object getProperty(String name) {
    Assert.notNull(name, "Property name can not be null.");
    Object value = source.getOrDefault(name, getValue(name));

    if (value == null) {
      return null;
    }

    if (isCloudBased(value)) {
      for (PropertyResolver propertyResolver : propertyResolverLoader.getPropertyResolvers()) {
        if (propertyResolver.supports(value.toString())) {
          Object resolvedValue = propertyResolver
            .resolve(value.toString())
            .doOnError(
              t -> {
                LOGGER.error("Unable to resolve property {}", name, t);
                source.put(name, null);
              }
            )
            .blockingGet(); // property must be resolved before continuing with the rest of the code
          source.put(name, resolvedValue); // to avoid resolving this property again

          watchProperty(propertyResolver, name, value);

          break;
        }
      }
    }

    return source.get(name);
  }

  protected abstract Object getValue(String key);

  private boolean isCloudBased(Object value) {
    for (CloudScheme cloudScheme : CloudScheme.values()) {
      if (value.toString().startsWith(cloudScheme.value())) {
        return true;
      }
    }

    return false;
  }

  private void watchProperty(
    PropertyResolver propertyResolver,
    String name,
    Object value
  ) {
    propertyResolver
      .watch(value.toString())
      .doOnNext(newValue -> source.put(name, newValue))
      .doOnError(t -> LOGGER.error("Unable to update property {}", name, t))
      .doOnComplete(() -> watchProperty(propertyResolver, name, value))
      .subscribe();
  }
}
