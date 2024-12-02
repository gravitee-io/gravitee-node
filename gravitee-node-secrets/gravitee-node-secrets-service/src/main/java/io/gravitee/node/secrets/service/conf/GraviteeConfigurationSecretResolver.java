package io.gravitee.node.secrets.service.conf;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.secrets.api.core.*;
import io.gravitee.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.gravitee.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.gravitee.secrets.api.util.ConfigHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Class to dispatch resolution "request" to the correct secret provider.
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class GraviteeConfigurationSecretResolver {

    private static final String SECRET_PROVIDER_NOT_FOUND_FOR_ID = "No secret-provider plugin found for provider id: '%s'";
    private static final String SECRETS_CONFIG_KEY = "secrets";

    private final SecretProviderPluginManager secretProviderPluginManager;
    private final Environment environment;
    private final Map<String, SecretProvider> secretProviders = new HashMap<>();
    private final Map<SecretLocation, SecretMap> secrets = Collections.synchronizedMap(new HashMap<>());

    @Getter
    @Accessors(fluent = true)
    private final List<String> enabledProviders = new ArrayList<>();

    public GraviteeConfigurationSecretResolver(SecretProviderPluginManager secretProviderPluginManager, Environment environment) {
        this.secretProviderPluginManager = secretProviderPluginManager;
        this.environment = environment;
        setupConverters((ConfigurableEnvironment) environment);
        secretProviderPluginManager.setOnNewPluginCallback(pluginId -> {
            if (isEnabled(pluginId)) {
                createAndRegister(pluginId);
                enabledProviders.add(pluginId);
            }
        });
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
     * Resolves a {@link SecretMap} from the correct secret provider
     *
     * @param secretMount the secret mount to resolve
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does not match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    public Maybe<SecretMap> resolve(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException {
        if (secrets.containsKey(secretMount.location())) {
            return Maybe.just(secrets.get(secretMount.location()));
        }
        return this.secretProviders.getOrDefault(secretMount.provider(), new ErrorSecretProvider())
            .resolve(secretMount)
            .subscribeOn(Schedulers.io())
            .doOnSuccess(secretMap -> secrets.put(secretMount.location(), secretMap));
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
            .orElseThrow(() -> new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(url.provider())));
    }

    /**
     * Delegates to {@link SecretProvider#resolve(SecretMount)} in order to resolve a {@link SecretMap}.
     * Then uses {@link SecretMount#key()} to extract the secret from the map.
     * An empty maybe is returned if resolution returns no secret or the key do not exist in the secret map.
     *
     * @param secretMount the secret mount to resolve
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does not match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    public Maybe<Secret> resolveKey(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException {
        if (secretMount.isKeyEmpty()) {
            return Maybe.error(new IllegalArgumentException("cannot request secret key, no key provided"));
        }
        return resolve(secretMount).flatMap(secretMap -> Maybe.fromOptional(secretMap.getSecret(secretMount)));
    }

    /**
     * Delegates to {@link SecretProvider#watch(SecretMount)} in order to watch a {@link SecretMap}.
     *
     * @param secretMount the secret mount to resolve
     * @param events      events to filter, <code>null</code> means "all"
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does not match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    public Flowable<SecretMap> watch(SecretMount secretMount, SecretEvent.Type... events) {
        final SecretProvider provider = secretProviders.getOrDefault(secretMount.provider(), new ErrorSecretProvider());
        return provider
            .watch(secretMount)
            .filter(secretEvent -> events == null || events.length == 0 || Arrays.asList(events).contains(secretEvent.type()))
            .subscribeOn(Schedulers.io())
            .map(SecretEvent::secretMap)
            .doFinally(provider::stop);
    }

    /**
     * Delegates to {@link SecretProvider#watch(SecretMount)} in order to resolve a {@link SecretMap}.
     * Then uses {@link SecretMount#key()} to extract the secret from the map.
     * No secret is published is none is found or the key do not exist in the secret map
     *
     * @param secretMount the secret mount to resolve
     * @param events      events to filter, null means "all"
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does not match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    public Flowable<Secret> watchKey(SecretMount secretMount, SecretEvent.Type... events) {
        if (secretMount.isKeyEmpty()) {
            return Flowable.error(new IllegalArgumentException("cannot request secret key, no key provided"));
        }
        return watch(secretMount, events).flatMapMaybe(secretMap -> Maybe.fromOptional(secretMap.getSecret(secretMount)));
    }

    // visible for tests
    Optional<SecretProvider> findSecretProvider(String id) {
        return Optional.ofNullable(secretProviders.get(id));
    }

    // visible for tests
    boolean isEnabled(String pluginId) {
        return environment.getProperty(String.format("%s.%s.enabled", SECRETS_CONFIG_KEY, pluginId), boolean.class, false);
    }

    private void createAndRegister(String id) {
        try {
            final SecretProviderPlugin<?, ?> secretProviderPlugin = secretProviderPluginManager.get(id);
            final Class<? extends SecretManagerConfiguration> configurationClass = secretProviderPlugin.configuration();
            final SecretProviderFactory<SecretManagerConfiguration> factory = secretProviderPluginManager.getFactoryById(id);
            if (configurationClass != null && factory != null) {
                // read the config using the plugin class loader
                SecretManagerConfiguration config =
                    this.readConfiguration(id, factory.getClass().getClassLoader().loadClass(configurationClass.getName()));
                // register and start
                secretProviders.put(id, factory.create(config).start());
            } else {
                throw new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(id));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("cannot load plugin %s properly: ".formatted(id));
        }
    }

    private void setupConverters(ConfigurableEnvironment environment) {
        environment
            .getConversionService()
            .addConverterFactory(
                new ConverterFactory<Secret, Object>() {
                    final ConversionService conversionService = DefaultConversionService.getSharedInstance();

                    @Nonnull
                    public <C> Converter<Secret, C> getConverter(@Nonnull Class<C> targetType) {
                        return source -> conversionService.convert(source.asString(), targetType);
                    }
                }
            );

        environment.getConversionService().addConverter(Secret.class, String.class, Secret::asString);
    }

    private boolean canProviderHandle(String location, String manager) {
        return location.startsWith("%s%s/".formatted(SecretURL.SCHEME, manager));
    }

    private <T extends SecretManagerConfiguration> T readConfiguration(String pluginId, Class<?> configurationClass) {
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

    private static class ErrorSecretProvider implements SecretProvider {

        @Override
        public Maybe<SecretMap> resolve(SecretMount secretMount) {
            return Maybe.error(new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(secretMount.provider())));
        }

        @Override
        public Flowable<SecretEvent> watch(SecretMount secretMount) {
            return Flowable.error(new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(secretMount.provider())));
        }

        @Override
        public SecretMount fromURL(SecretURL url) {
            throw new SecretProviderNotFoundException("No secret provider plugin found for url: " + url);
        }
    }

    // for tests
    Map<SecretLocation, SecretMap> secrets() {
        return Map.copyOf(secrets);
    }
}
