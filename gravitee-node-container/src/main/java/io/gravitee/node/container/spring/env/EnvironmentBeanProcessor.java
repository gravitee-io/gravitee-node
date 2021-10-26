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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentBeanProcessor
  implements BeanFactoryPostProcessor, Ordered {

  private static final String[] PROPERTY_PREFIXES = new String[] {
    "gravitee.",
    "gravitee_",
    "GRAVITEE.",
    "GRAVITEE_",
  };
  private final Environment environment;
  private final ApplicationContext applicationContext;

  EnvironmentBeanProcessor(
    Environment environment,
    ApplicationContext applicationContext
  ) {
    this.environment = environment;
    this.applicationContext = applicationContext;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void postProcessBeanFactory(
    ConfigurableListableBeanFactory beanFactory
  ) {
    Map<String, Object> source = new ConcurrentHashMap<>();
    ((StandardEnvironment) environment).getSystemEnvironment()
      .forEach(
        (key, value) -> {
          for (String propertyPrefix : PROPERTY_PREFIXES) {
            if (key.startsWith(propertyPrefix)) {
              source.put(key.substring(propertyPrefix.length()), value);
            }
          }
        }
      );

    ((ConfigurableEnvironment) environment).getPropertySources()
      .addFirst(
        new GraviteePropertySource(
          "graviteeEnvironmentPropertySource",
          source,
          applicationContext
        )
      );
  }
}
