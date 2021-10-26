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

import java.util.Properties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import({ PropertiesConfiguration.class })
public class EnvironmentConfiguration {

  @Bean
  public static PropertySourcesPlaceholderConfigurer properties(
    @Qualifier("graviteeProperties") Properties graviteeProperties
  ) {
    PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
    propertySourcesPlaceholderConfigurer.setProperties(graviteeProperties);
    propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(
      true
    );

    return propertySourcesPlaceholderConfigurer;
  }

  @Bean
  public static PropertySourceBeanProcessor propertySourceBeanProcessor(
    @Qualifier("graviteeProperties") Properties graviteeProperties,
    Environment environment,
    ApplicationContext applicationContext
  ) {
    // Using this we are now able to use {@link org.springframework.core.env.Environment} in Spring beans
    return new PropertySourceBeanProcessor(
      graviteeProperties,
      environment,
      applicationContext
    );
  }

  @Bean
  public static EnvironmentBeanProcessor environmentBeanFactoryPostProcessor(
    Environment environment,
    ApplicationContext applicationContext
  ) {
    return new EnvironmentBeanProcessor(environment, applicationContext);
  }
}
