package io.gravitee.node.secrets.service.conf;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.node.secrets.SecretProviderPluginManager;
import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.model.SecretMount;
import io.gravitee.node.secrets.api.model.SecretURL;
import io.gravitee.node.secrets.api.util.ConfigHelper;
import io.gravitee.node.secrets.service.AbstractSecretProviderDispatcher;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 *
 * @author GraviteeSource Team
 */
@Slf4j
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
        // can't use lambdas here or Spring complains because it cannot infer types
        environment
            .getConversionService()
            .addConverter(
                new Converter<Secret, String>() {
                    @Override
                    public String convert(@Nonnull Secret source) {
                        return source.asString();
                    }
                }
            );
        environment
            .getConversionService()
            .addConverter(
                new Converter<Secret, byte[]>() {
                    @Override
                    public byte[] convert(@Nonnull Secret source) {
                        return source.asBytes();
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
        Map<String, Object> configurationProperties = ConfigHelper.removePrefix(
            EnvironmentUtils.getAllProperties((ConfigurableEnvironment) environment),
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

    public boolean canHandle(String location) {
        Objects.requireNonNull(location);
        return enabledManagers().stream().anyMatch(manager -> canManagerHande(location, manager));
    }

    public boolean isResolvable(String location) {
        Objects.requireNonNull(location);
        return enabledManagers()
            .stream()
            .anyMatch(manager -> {
                try {
                    SecretMount secretMount = toSecretMount(location);
                    return canManagerHande(location, manager) && !secretMount.isKeyEmpty();
                } catch (IllegalArgumentException | SecretProviderNotFoundException e) {
                    // URL might not be suitable for resolving property
                    return false;
                }
            });
    }

    public SecretMount toSecretMount(String location) {
        SecretURL url = SecretURL.from(location);
        return this.findSecretProvider(url.provider())
            .map(secretProvider -> {
                try {
                    return secretProvider.fromURL(url);
                } catch (IllegalArgumentException e) {
                    throw new SecretManagerConfigurationException("cannot create secret URL from: " + location, e);
                }
            })
            .orElseThrow(() ->
                new SecretProviderNotFoundException(
                    AbstractSecretProviderDispatcher.SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(url.provider())
                )
            );
    }

    private static boolean canManagerHande(String location, String manager) {
        return location.startsWith("%s%s/".formatted(SecretURL.SCHEME, manager));
    }
}
