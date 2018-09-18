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
package io.gravitee.node.notifier.internal;

import com.google.common.base.Predicate;
import io.gravitee.notifier.api.NotificationConfiguration;
import io.gravitee.notifier.api.Notifier;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.reflections.ReflectionUtils.*;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class NotifierFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierFactory.class);

    public Notifier create(Class<? extends Notifier> notifierClass, Map<Class<?>, Object> injectables) throws Exception {
        LOGGER.debug("Create a new notifier instance for {}", notifierClass.getName());

        return createNotifier(notifierClass, injectables);
    }

    private Notifier createNotifier(Class<? extends Notifier> notifierClass, Map<Class<?>, Object> injectables) {
        Notifier notifierInst = null;

        Constructor<? extends Notifier> constr = lookingForConstructor(notifierClass);

        if (constr != null) {
            try {
            //    if (constr.getParameterCount() > 0) {
            //        notifierInst = constr.newInstance(injectables.get(policyMetadata.configuration()));
            //    } else {
                    notifierInst = constr.newInstance();
                //    }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                LOGGER.error("Unable to instantiate notifier {}", notifierClass, ex);
            }
        }

        if (notifierInst != null) {
            Set<Field> fields = lookingForInjectableFields(notifierClass);
            if (fields != null) {
                for (Field field : fields) {
                    boolean accessible = field.isAccessible();

                    Class<?> type = field.getType();
                    Optional<?> value = injectables.values().stream()
                            .filter(o -> type.isAssignableFrom(o.getClass()))
                            .findFirst();

                    if (value.isPresent()) {
                        LOGGER.debug("Inject value into field {} [{}] in {}", field.getName(), type.getName(), notifierClass);
                        try {
                            field.setAccessible(true);
                            field.set(notifierInst, value.get());
                        } catch (IllegalAccessException iae) {
                            LOGGER.error("Unable to set field value for {} in {}", field.getName(), notifierClass, iae);
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
        LOGGER.debug("Looking for a constructor to inject notifier configuration");
        Constructor <? extends Notifier> constructor = null;

        Set<Constructor> notifierConstructors =
                ReflectionUtils.getConstructors(notifierClass,
                        withModifier(Modifier.PUBLIC),
                        withParametersAssignableFrom(NotificationConfiguration.class),
                        withParametersCount(1));

        if (notifierConstructors.isEmpty()) {
            LOGGER.debug("No configuration can be injected for {} because there is no valid constructor. " +
                    "Using default empty constructor.", notifierClass.getName());
            try {
                constructor = notifierClass.getConstructor();
            } catch (NoSuchMethodException nsme) {
                LOGGER.error("Unable to find default empty constructor for {}", notifierClass.getName(), nsme);
            }
        } else if (notifierConstructors.size() == 1) {
            constructor = notifierConstructors.iterator().next();
        } else {
            LOGGER.info("Too much constructors to instantiate notifier {}", notifierClass.getName());
        }

        return constructor;
    }

    private Set<Field> lookingForInjectableFields(Class<?> notifierClass) {
        return ReflectionUtils.getAllFields(notifierClass, withAnnotation(Inject.class));
    }

    public static Predicate<Member> withParametersAssignableFrom(final Class... types) {
        return input -> {
            if (input != null) {
                Class<?>[] parameterTypes = parameterTypes(input);
                if (parameterTypes.length == types.length) {
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (!types[i].isAssignableFrom(parameterTypes[i]) ||
                                (parameterTypes[i] == Object.class && types[i] != Object.class)) {
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
        return member != null ?
                member.getClass() == Method.class ? ((Method) member).getParameterTypes() :
                        member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null : null;
    }
}
