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
package io.gravitee.node.notifier.plugin.impl;

import static org.reflections.ReflectionUtils.*;

import com.google.common.base.Predicate;
import io.gravitee.node.api.notifier.NotificationDefinition;
import io.gravitee.node.notifier.plugin.NotifierPluginConfigurationFactory;
import io.gravitee.node.notifier.plugin.NotifierPluginFactory;
import io.gravitee.notifier.api.Notifier;
import io.gravitee.notifier.api.NotifierConfiguration;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.notifier.NotifierClassLoaderFactory;
import io.gravitee.plugin.notifier.NotifierPlugin;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotifierPluginFactoryImpl implements NotifierPluginFactory {

    private final Logger logger = LoggerFactory.getLogger(NotifierPluginFactoryImpl.class);

    @Autowired
    private ConfigurablePluginManager<NotifierPlugin> notifierManager;

    @Autowired
    private NotifierPluginConfigurationFactory notifierConfigurationFactory;

    @Autowired
    private NotifierClassLoaderFactory notifierClassLoaderFactory;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Optional<Notifier> create(NotificationDefinition notification) {
        logger.debug("Create a new notifier instance for {}", notification.getType());
        NotifierPlugin plugin = notifierManager.get(notification.getType());

        if (plugin == null) {
            logger.error("No notifier plugin is available to handle notification's type: {}", notification.getType());
            return Optional.empty();
        }

        PluginClassLoader notifierClassLoader = notifierClassLoaderFactory.getOrCreateClassLoader(plugin);

        try {
            NotifierConfiguration configuration = notifierConfigurationFactory.create(
                (Class<? extends NotifierConfiguration>) ClassUtils.forName(plugin.configuration().getName(), notifierClassLoader),
                notification.getConfiguration()
            );

            return Optional.ofNullable(
                create((Class<? extends Notifier>) ClassUtils.forName(plugin.clazz(), notifierClassLoader), configuration)
            );
        } catch (ClassNotFoundException e) {
            logger.error("Unable to instantiate the class {}", plugin.clazz(), e);
        }

        return Optional.empty();
    }

    private <C extends NotifierConfiguration> Notifier create(Class<? extends Notifier> notifierClass, C notifierConfiguration) {
        Notifier notifierInst = null;

        Constructor<? extends Notifier> constr = lookingForConstructor(notifierClass);

        if (constr != null) {
            try {
                notifierInst = constr.newInstance(notifierConfiguration);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                logger.error("Unable to instantiate notifier {}", notifierClass, ex);
            }
        }

        if (notifierInst != null) {
            applicationContext.getAutowireCapableBeanFactory().autowireBean(notifierInst);

            if (notifierInst instanceof ApplicationContextAware) {
                ((ApplicationContextAware) notifierInst).setApplicationContext(applicationContext);
            }

            Set<Field> fields = lookingForInjectableFields(notifierClass);
            if (fields != null) {
                for (Field field : fields) {
                    boolean accessible = field.isAccessible();
                    Map<Class<?>, Object> injectables = new HashMap<>();
                    injectables.put(notifierConfiguration.getClass(), notifierConfiguration);

                    Class<?> type = field.getType();
                    Optional<?> value = injectables.values().stream().filter(o -> type.isAssignableFrom(o.getClass())).findFirst();

                    if (value.isPresent()) {
                        logger.debug("Inject value into field {} [{}] in {}", field.getName(), type.getName(), notifierClass);
                        try {
                            field.setAccessible(true);
                            field.set(notifierInst, value.get());
                        } catch (IllegalAccessException iae) {
                            logger.error("Unable to set field value for {} in {}", field.getName(), notifierClass, iae);
                        } finally {
                            field.setAccessible(accessible);
                        }
                    }
                }
            }
        }

        return notifierInst;
    }

    private Constructor<? extends Notifier> lookingForConstructor(Class<? extends Notifier> notifierClass) {
        logger.debug("Looking for a constructor to inject notifier configuration");
        Constructor<? extends Notifier> constructor = null;

        Set<Constructor> resourceConstructors = ReflectionUtils.getConstructors(
            notifierClass,
            withModifier(Modifier.PUBLIC),
            withParametersAssignableFrom(NotifierConfiguration.class),
            withParametersCount(1)
        );

        if (resourceConstructors.isEmpty()) {
            logger.debug(
                "No configuration can be injected for {} because there is no valid constructor. " + "Using default empty constructor.",
                notifierClass.getName()
            );
            try {
                constructor = notifierClass.getConstructor();
            } catch (NoSuchMethodException nsme) {
                logger.error("Unable to find default empty constructor for {}", notifierClass.getName(), nsme);
            }
        } else if (resourceConstructors.size() == 1) {
            constructor = resourceConstructors.iterator().next();
        } else {
            logger.info("Too much constructors to instantiate notifier {}", notifierClass.getName());
        }

        return constructor;
    }

    private Set<Field> lookingForInjectableFields(Class<?> resourceClass) {
        return ReflectionUtils.getAllFields(resourceClass, withAnnotation(Inject.class));
    }

    public static Predicate<Member> withParametersAssignableFrom(final Class... types) {
        return input -> {
            if (input != null) {
                Class<?>[] parameterTypes = parameterTypes(input);
                if (parameterTypes.length == types.length) {
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (
                            !types[i].isAssignableFrom(parameterTypes[i]) || (parameterTypes[i] == Object.class && types[i] != Object.class)
                        ) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        };
    }

    private static Class[] parameterTypes(Member member) {
        return member != null
            ? member.getClass() == Method.class
                ? ((Method) member).getParameterTypes()
                : member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null
            : null;
    }
}
