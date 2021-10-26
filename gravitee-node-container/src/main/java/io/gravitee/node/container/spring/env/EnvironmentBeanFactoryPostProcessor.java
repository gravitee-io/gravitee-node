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

import io.gravitee.common.util.RelaxedPropertySource;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.spring.KubernetesClientConfiguration;
import io.gravitee.node.kubernetes.propertyresolver.CloudScheme;
import io.gravitee.node.kubernetes.propertyresolver.PropertyResolver;
import io.gravitee.node.kubernetes.propertyresolver.PropertyResolverFactoriesLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentBeanFactoryPostProcessor
  implements BeanFactoryPostProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    EnvironmentBeanFactoryPostProcessor.class
  );
  private static final String[] PROPERTY_PREFIXES = new String[] {
    "gravitee.",
    "gravitee_",
    "GRAVITEE.",
    "GRAVITEE_",
  };

  private final Environment environment;
  private final PropertyResolverFactoriesLoader propertyResolverLoader;

  public EnvironmentBeanFactoryPostProcessor(
    Environment environment,
    ApplicationContext applicationContext
  ) {
    this.environment = environment;
    this.propertyResolverLoader =
      applicationContext.getBean(PropertyResolverFactoriesLoader.class);
  }

  @Override
  public void postProcessBeanFactory(
    ConfigurableListableBeanFactory beanFactory
  ) throws BeansException {
    if (environment != null) {
      Map<String, Object> systemEnvironment =
        ((StandardEnvironment) environment).getSystemEnvironment();
      Map<String, Object> prefixlessSystemEnvironment = new HashMap<>(
        systemEnvironment.size()
      );
      systemEnvironment.forEach(
        (key, value) -> {
          for (String propertyPrefix : PROPERTY_PREFIXES) {
            if (key.startsWith(propertyPrefix)) {
              if (isCloudBased(value)) {
                for (PropertyResolver propertyResolver : propertyResolverLoader.getPropertyResolvers()) {
                  if (propertyResolver.supports(value.toString())) {
                    Object resolvedValue = propertyResolver
                      .resolve(value.toString())
                      .doOnError(
                        t -> {
                          LOGGER.error(
                            "Unable to resolve property {}",
                            key.substring(propertyPrefix.length()),
                            t
                          );
                          prefixlessSystemEnvironment.put(
                            key.substring(propertyPrefix.length()),
                            null
                          ); // to avoid resolving this property again
                        }
                      )
                      .blockingGet(); // property must be resolved before continuing with the rest of the code
                    prefixlessSystemEnvironment.put(
                      key.substring(propertyPrefix.length()),
                      resolvedValue
                    ); // to avoid resolving this property again

                    watchProperty(
                      propertyResolver,
                      prefixlessSystemEnvironment,
                      key.substring(propertyPrefix.length()),
                      value
                    );

                    break;
                  }
                }
              }

              break;
            }
          }
        }
      );

      ((StandardEnvironment) environment).getPropertySources()
        .replace(
          StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
          new RelaxedPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            prefixlessSystemEnvironment
          )
        );
    }
  }

  private boolean isCloudBased(Object value) {
    if (value == null) {
      return false;
    }

    for (CloudScheme cloudScheme : CloudScheme.values()) {
      if (value.toString().startsWith(cloudScheme.value())) {
        return true;
      }
    }

    return false;
  }

  private void watchProperty(
    PropertyResolver propertyResolver,
    Map<String, Object> prefixlessSystemEnvironment,
    String name,
    Object value
  ) {
    propertyResolver
      .watch(value.toString())
      .doOnNext(newValue -> prefixlessSystemEnvironment.put(name, newValue))
      .doOnError(t -> LOGGER.error("Unable to update property {}", name, t))
      .doOnComplete(
        () ->
          watchProperty(
            propertyResolver,
            prefixlessSystemEnvironment,
            name,
            value
          )
      )
      .subscribe();
  }
}
