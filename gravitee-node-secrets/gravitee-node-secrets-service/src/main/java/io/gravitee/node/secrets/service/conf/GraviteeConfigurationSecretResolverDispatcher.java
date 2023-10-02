package io.gravitee.node.secrets.service.conf;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.errors.SecretManagerConfigurationException;
import io.gravitee.node.api.secrets.errors.SecretManagerException;
import io.gravitee.node.api.secrets.errors.SecretProviderNotFoundException;
import io.gravitee.node.api.secrets.model.*;
import io.gravitee.node.api.secrets.util.ConfigHelper;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.node.secrets.service.AbstractSecretProviderDispatcher;
import io.reactivex.rxjava3.core.Maybe;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class GraviteeConfigurationSecretResolverDispatcher extends AbstractSecretProviderDispatcher {

    private static final String SECRETS_CONFIG_KEY = "secrets";
    private final Environment environment;

    private final Map<SecretLocation, SecretMap> secrets = Collections.synchronizedMap(new HashMap<>());

    @Getter
    @Accessors(fluent = true)
    private final List<String> enabledProviders = new ArrayList<>();

    public GraviteeConfigurationSecretResolverDispatcher(SecretProviderPluginManager secretProviderPluginManager, Environment environment) {
        super(secretProviderPluginManager);
        this.environment = environment;
        setupConverters((ConfigurableEnvironment) environment);
        secretProviderPluginManager.setOnNewPluginCallback(pluginId -> {
            if (isEnabled(pluginId)) {
                super.createAndRegister(pluginId);
                enabledProviders.add(pluginId);
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
            "%s.%s".formatted(SECRETS_CONFIG_KEY, pluginId)
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

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException {
        if (secrets.containsKey(secretMount.location())) {
            return Maybe.just(secrets.get(secretMount.location()));
        }
        return super.resolve(secretMount).doOnSuccess(secretMap -> secrets.put(secretMount.location(), secretMap));
    }

    /**
     * Check if the value given can be handled by a provider.
     *
     * @param location the URL of a secret
     * @return true if there is a provider able to handle this URL
     */
    public boolean canHandle(String location) {
        Objects.requireNonNull(location);
        return (
            location.startsWith(SecretProvider.PLUGIN_URL_SCHEME) &&
            enabledProviders().stream().anyMatch(manager -> canProviderHandle(location, manager))
        );
    }

    /**
     * Check if the value given can be handled by a provider and if the URL can to be use to resolve a single value
     *
     * @param location the URL of a secret
     * @return true if they location can return a single secret
     */
    public boolean canResolveSingleValue(String location) {
        Objects.requireNonNull(location);
        if (canHandle(location)) {
            try {
                SecretMount secretMount = toSecretMount(location);
                if (secretMount.isKeyEmpty()) {
                    throw new IllegalArgumentException(
                        "Secret URL should must specify a 'key' in order to resolve a single value, such as: %s:<KEY>".formatted(location)
                    );
                }
                return true;
            } catch (IllegalArgumentException | SecretProviderNotFoundException e) {
                // URL might not be suitable for resolving property
                return false;
            }
        }
        return false;
    }

    /**
     * Uses a secret provider to convert a URL to {@link SecretMount}
     *
     * @param location secret location
     * @return a {@link SecretMount}
     * @throws SecretProviderNotFoundException     if the URL points a non-existing secret provider
     * @throws SecretManagerConfigurationException if the URL processing led to an error
     * @throws IllegalArgumentException            if the URL is well formatted
     */
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

    // for tests
    Map<SecretLocation, SecretMap> secrets() {
        return Map.copyOf(secrets);
    }

    private static boolean canProviderHandle(String location, String manager) {
        return location.startsWith("%s%s/".formatted(SecretURL.SCHEME, manager));
    }
}
