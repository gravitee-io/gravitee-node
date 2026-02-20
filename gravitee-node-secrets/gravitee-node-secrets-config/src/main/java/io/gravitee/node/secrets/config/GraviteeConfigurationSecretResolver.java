package io.gravitee.node.secrets.config;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.secrets.api.core.Secret;
import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.gravitee.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.gravitee.secrets.api.util.ConfigHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.annotation.Nonnull;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Resolve a secret for Gravitee configuration. It selects a secret-provider plugin and call it.
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class GraviteeConfigurationSecretResolver {

    private static final String SECRET_PROVIDER_NOT_FOUND_FOR_ID = "No secret-provider plugin found for provider id: '%s'";
    private static final String SECRETS_CONFIG_KEY = "secrets";

    private final SecretProviderPluginManager secretProviderPluginManager;
    private final Environment environment;
    private final Map<String, SecretProvider> secretProviders = new HashMap<>();
    private final Map<String, SecretMap> secrets = Collections.synchronizedMap(new HashMap<>());

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
     * Check if given location can be handled by a provider.
     *
     * @param location the URL of a secret as a String
     * @return true if there is a provider is able to handle this URL
     */
    public boolean canHandle(String location) {
        Objects.requireNonNull(location);
        return (
            location.startsWith(SecretProvider.PLUGIN_URL_SCHEME) &&
            enabledProviders().stream().anyMatch(pluginId -> location.startsWith("%s%s/".formatted(SecretURL.SCHEME, pluginId)))
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
                SecretURL secretURL = asSecretURL(location);
                if (secretURL.isKeyEmpty()) {
                    throw new IllegalArgumentException(
                        "Secret URL must specify a 'key' in order to resolve a single value, it should like this '%s:<KEY>'".formatted(
                                location
                            )
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
     * Resolves a {@link SecretMap} from a secret provider
     *
     * @param secretURL secret URL
     * @return a maybe secret map
     * @throws SecretProviderNotFoundException (as a Single.error()) if the {@link SecretURL#provider()} does not match an enabled secret provider plugin
     * @see SecretProvider#resolve(SecretURL)
     */
    public Single<SecretMap> resolve(SecretURL secretURL) {
        String cacheKey = secretURL
            .query()
            .get(SecretURL.WellKnownQueryParam.NAMESPACE)
            .stream()
            .findFirst()
            .map(ns -> ns.concat("-"))
            .orElse("")
            .concat(secretURL.path());

        if (secrets.containsKey(cacheKey)) {
            return Single.just(secrets.get(cacheKey));
        }
        return this.secretProviders.getOrDefault(secretURL.provider(), new ErrorSecretProvider())
            .resolve(secretURL)
            .switchIfEmpty(Single.error(new SecretManagerException("secret not found: ".concat(secretURL.path()))))
            .subscribeOn(Schedulers.io())
            .doOnSuccess(secretMap -> secrets.put(cacheKey, secretMap));
    }

    /**
     * Parse URL and check that provider exists for it
     *
     * @param location secret location
     * @return a {@link SecretURL}
     * @throws SecretProviderNotFoundException     if the URL points a non-existing secret provider
     * @throws SecretManagerConfigurationException if the URL processing led to an error
     * @throws IllegalArgumentException            if the URL is well formatted
     */

    public SecretURL asSecretURL(String location) {
        SecretURL url = SecretURL.from(location);
        return this.findSecretProvider(url.provider())
            .map(secretProvider -> url)
            .orElseThrow(() -> new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(url.provider())));
    }

    /**
     * Delegates to {@link SecretProvider#resolve(SecretURL)} in order to resolve a {@link SecretMap}.
     * Then uses {@link SecretURL#key()} to extract the secret from the map.
     * An empty maybe is returned if resolution returns no secret or the key do not exist in the secret map.
     *
     * @param secretURL url to resolve
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretURL#provider()} does not match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    public Maybe<Secret> resolveKey(SecretURL secretURL) throws SecretProviderNotFoundException, SecretManagerException {
        if (secretURL.isKeyEmpty()) {
            return Maybe.error(new IllegalArgumentException("cannot request secret key, no key provided"));
        }
        return resolve(secretURL)
            .flatMapMaybe(secretMap -> {
                Optional<Secret> secret = secretMap.getSecret(secretURL);

                if (secretURL.isExistenceCheck()) {
                    return Maybe.just(new Secret(String.valueOf(secret.isPresent())));
                }

                if (secret.isPresent()) {
                    return Maybe.just(secret.get());
                }

                return secretURL.getFallback().map(fallbackValue -> Maybe.just(new Secret(fallbackValue))).orElse(Maybe.empty());
            })
            .onErrorResumeNext(throwable -> {
                if (secretURL.isExistenceCheck()) {
                    return Maybe.just(new Secret("false"));
                }
                return Maybe.error(throwable);
            });
    }

    /**
     * Delegates to {@link SecretProvider#watch(SecretURL)} in order to watch a {@link SecretMap}.
     *
     * @param secretURL url to resolve
     * @param events    events to filter, <code>null</code> means "all"
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretURL#provider()} does not match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    public Flowable<SecretMap> watch(SecretURL secretURL, SecretEvent.Type... events) {
        final SecretProvider provider = secretProviders.getOrDefault(secretURL.provider(), new ErrorSecretProvider());
        return provider
            .watch(secretURL)
            .filter(secretEvent -> events == null || events.length == 0 || Arrays.asList(events).contains(secretEvent.type()))
            .subscribeOn(Schedulers.io())
            .map(SecretEvent::secretMap)
            .doFinally(provider::stop);
    }

    /**
     * Delegates to {@link SecretProvider#watch(SecretURL)} in order to resolve a {@link SecretMap}.
     * Then uses {@link SecretURL#key()} to extract the secret from the map.
     * No secret is published is none is found or the key do not exist in the secret map
     *
     * @param secretURL url to resolve
     * @param events    events to filter, null means "all"
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretURL#provider()} does not match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    public Flowable<Secret> watchKey(SecretURL secretURL, SecretEvent.Type... events) {
        if (secretURL.isKeyEmpty()) {
            return Flowable.error(new IllegalArgumentException("cannot request secret key, no key provided"));
        }
        return watch(secretURL, events).flatMapMaybe(secretMap -> Maybe.fromOptional(secretMap.getSecret(secretURL)));
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
        public Maybe<SecretMap> resolve(SecretURL secretURL) {
            return Maybe.error(new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(secretURL.provider())));
        }

        @Override
        public Flowable<SecretEvent> watch(SecretURL secretURL) {
            return Flowable.error(new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(secretURL.provider())));
        }
    }

    // for tests
    Map<String, SecretMap> secrets() {
        return Map.copyOf(secrets);
    }
}
