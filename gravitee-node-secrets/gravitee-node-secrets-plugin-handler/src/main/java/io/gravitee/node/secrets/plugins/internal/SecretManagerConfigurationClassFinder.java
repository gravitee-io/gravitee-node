package io.gravitee.node.secrets.plugins.internal;

import io.gravitee.plugin.core.api.AbstractSingleSubTypesFinder;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecretManagerConfigurationClassFinder extends AbstractSingleSubTypesFinder<SecretManagerConfiguration> {

    protected SecretManagerConfigurationClassFinder() {
        super(SecretManagerConfiguration.class);
    }

    @Override
    public Collection<Class<? extends SecretManagerConfiguration>> lookup(Class<?> clazz, ClassLoader classLoader) {
        log.debug("Looking for a configuration class for secret provider {} in package {}", clazz.getName(), clazz.getPackage().getName());
        Collection<Class<? extends SecretManagerConfiguration>> configurations = super.lookup(clazz, classLoader);

        if (configurations.isEmpty()) {
            log.info("No secret provider configuration class defined for endpoint {}", clazz.getName());
        }

        return configurations;
    }
}
