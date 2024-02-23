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
package io.gravitee.node.container.spring;

import io.gravitee.node.container.ContainerInitializer;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Special Spring {@link BeanDefinitionRegistryPostProcessor} that can be implemented to auto-detect and register {@link ContainerInitializer}
 * beans into the Spring registry.
 * It differs from {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor} in a sense that it allows for registering beans
 * before the Spring context has been refreshed and allows for support of {@link org.springframework.context.annotation.Import} annotation.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
class ContainerInitializerBeanRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    @Override
    public void postProcessBeanDefinitionRegistry(@Nonnull BeanDefinitionRegistry registry) throws BeansException {
        final AnnotatedBeanDefinitionReader annotatedBeanDefinitionReader = new AnnotatedBeanDefinitionReader(registry);
        final List<? extends Class<?>> containerInitializers = SpringFactoriesLoader
            .loadFactories(ContainerInitializer.class, this.getClass().getClassLoader())
            .stream()
            .map(ContainerInitializer::getClass)
            .toList();

        for (Class<?> containerInitializerClass : containerInitializers) {
            annotatedBeanDefinitionReader.registerBean(containerInitializerClass, containerInitializerClass.getName());
        }
    }

    @Override
    public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory) throws BeansException {}

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
