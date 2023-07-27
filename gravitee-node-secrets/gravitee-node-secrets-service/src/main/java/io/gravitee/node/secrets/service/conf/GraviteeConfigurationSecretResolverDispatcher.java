package io.gravitee.node.secrets.service.conf;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.node.secrets.SecretProviderPluginManager;
import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.util.ConfigHelper;
import io.gravitee.node.secrets.service.AbstractSecretProviderDispatcher;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeConfigurationSecretResolverDispatcher extends AbstractSecretProviderDispatcher {

    private static final String SECRETS_CONFIG_KEY = "secrets";
    private final Environment environment;

    @Getter
    @Accessors(fluent = true)
    private final List<String> enabledManagers = new ArrayList<>();

    public GraviteeConfigurationSecretResolverDispatcher(SecretProviderPluginManager secretProviderPluginManager, Environment environment) {
        super(secretProviderPluginManager);
        this.environment = environment;
        setupConverters((ConfigurableEnvironment) environment);
        secretProviderPluginManager.setOnNewPluginCallback(pluginId -> {
            if (isEnabled(pluginId)) {
                super.createAndRegister(pluginId);
                enabledManagers.add(pluginId);
            }
        });
    }

    @SuppressWarnings("java:S1604")
    private void setupConverters(ConfigurableEnvironment environment) {
        // can't use lambdas here or Spring complains
        environment
            .getConversionService()
            .addConverter(
                new Converter<Secret, String>() {
                    @Override
                    public String convert(Secret source) {
                        return new String(source.value(), StandardCharsets.UTF_8);
                    }
                }
            );
        environment
            .getConversionService()
            .addConverter(
                new Converter<Secret, byte[]>() {
                    @Override
                    public byte[] convert(Secret source) {
                        return source.value();
                    }
                }
            );
    }

    @Override
    public boolean isEnabled(String pluginId) {
        return environment.getProperty(String.format("%s.%s.enabled", SECRETS_CONFIG_KEY, pluginId), boolean.class, false);
    }

    @Override
    public <T extends SecretManagerConfiguration> T readConfiguration(String pluginId, Class<?> configurationClass) {
        Map<String, Object> configurationProperties = getChoppedPropertiesStartingWith(
            (ConfigurableEnvironment) environment,
            String.format("%s.%s", SECRETS_CONFIG_KEY, pluginId)
        );

        try {
            @SuppressWarnings("unchecked")
            Constructor<T> constructor = (Constructor<T>) configurationClass.getDeclaredConstructor(Map.class);
            return constructor.newInstance(configurationProperties);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new SecretManagerConfigurationException(
                "Could not create configuration class for secret manager: %s".formatted(pluginId),
                e
            );
        }
    }

    private Map<String, Object> getChoppedPropertiesStartingWith(ConfigurableEnvironment env, String prefix) {
        Map<String, Object> result = EnvironmentUtils.getAllProperties(env);
        return ConfigHelper.removePrefix(result, prefix);
    }
}
