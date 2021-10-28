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
import java.util.Properties;
import java.util.stream.Collectors;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PropertySourceBeanProcessor
  implements BeanFactoryPostProcessor, Ordered {

  private final ApplicationContext applicationContext;
  private final Environment environment;
  private final Properties properties;

  PropertySourceBeanProcessor(
    Properties properties,
    Environment environment,
    ApplicationContext applicationContext
  ) {
    this.properties = properties;
    this.environment = environment;
    this.applicationContext = applicationContext;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  @Override
  public void postProcessBeanFactory(
    ConfigurableListableBeanFactory beanFactory
  ) {
    Map<String, Object> source = properties
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(
          entry -> entry.getKey().toString(),
          Map.Entry::getValue
        )
      );

    ((ConfigurableEnvironment) environment).getPropertySources()
      .addLast(
        new GraviteeYamlPropertySource(
          "graviteeYamlConfiguration",
          source,
          applicationContext
        )
      );
  }
}
