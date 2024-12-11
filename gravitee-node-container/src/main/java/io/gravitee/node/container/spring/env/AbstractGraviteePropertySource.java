/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.container.spring.env;

import io.gravitee.common.utils.RxHelper;
import io.gravitee.node.api.resolver.PropertyResolver;
import io.gravitee.node.api.resolver.PropertyResolverFactoriesLoader;
import io.gravitee.node.api.resolver.WatchablePropertyResolver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.Assert;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGraviteePropertySource extends EnumerablePropertySource<Map<String, Object>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGraviteePropertySource.class);
    private final PropertyResolverFactoriesLoader propertyResolverLoader;

    protected AbstractGraviteePropertySource(String name, Map<String, Object> source, ApplicationContext applicationContext) {
        super(name, source);
        this.propertyResolverLoader = applicationContext.getBean(PropertyResolverFactoriesLoader.class);
    }

    @Override
    public String[] getPropertyNames() {
        return source.keySet().toArray(new String[0]);
    }

    @Override
    public Object getProperty(String name) {
        Assert.notNull(name, "Property name can not be null.");
        Object value = source.getOrDefault(name, getValue(name));

        if (value == null) {
            return null;
        }

        for (PropertyResolver<?> propertyResolver : propertyResolverLoader.getPropertyResolvers()) {
            if (propertyResolver.supports(value.toString())) {
                Object resolvedValue = propertyResolver
                    .resolve(value.toString())
                    .doOnError(t -> {
                        LOGGER.error("Unable to resolve property {}", name, t);
                        source.put(name, null);
                    })
                    .blockingGet(); // property must be resolved before continuing with the rest of the code
                source.put(name, resolvedValue); // to avoid resolving this property again

                if (propertyResolver instanceof WatchablePropertyResolver<?> wpr && wpr.isWatchable(value.toString())) {
                    watchProperty(wpr, name, value);
                }

                break;
            }
        }

        return getValue(name);
    }

    protected abstract Object getValue(String key);

    private void watchProperty(WatchablePropertyResolver<?> propertyResolver, String name, Object value) {
        propertyResolver
            .watch(value.toString())
            .retryWhen(RxHelper.retryExponentialBackoff(1, TimeUnit.SECONDS))
            .subscribeOn(Schedulers.io())
            .subscribe(newValue -> source.put(name, newValue), t -> LOGGER.error("Unable to update property {}", name, t));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AbstractGraviteePropertySource that = (AbstractGraviteePropertySource) o;
        return Arrays.equals(getPropertyNames(), that.getPropertyNames());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyResolverLoader);
    }
}
