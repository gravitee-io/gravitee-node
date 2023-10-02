package io.gravitee.node.secrets.service;

import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.SecretProviderDispatcher;
import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.node.api.secrets.errors.SecretManagerException;
import io.gravitee.node.api.secrets.errors.SecretProviderNotFoundException;
import io.gravitee.node.api.secrets.model.*;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class AbstractSecretProviderDispatcher implements SecretProviderDispatcher {

    public static final String SECRET_PROVIDER_NOT_FOUND_FOR_ID = "No secret-provider plugin found for provider id: '%s'";
    private final SecretProviderPluginManager secretProviderPluginManager;

    private final Map<String, SecretProvider> secretProviders = new HashMap<>();

    protected AbstractSecretProviderDispatcher(SecretProviderPluginManager secretProviderPluginManager) {
        this.secretProviderPluginManager = secretProviderPluginManager;
    }

    protected final void createAndRegister(String id) {
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

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException {
        return secretProviders
            .getOrDefault(secretMount.provider(), new ErrorSecretProvider())
            .resolve(secretMount)
            .subscribeOn(Schedulers.io());
    }

    @Override
    public Maybe<Secret> resolveKey(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException {
        if (secretMount.isKeyEmpty()) {
            return Maybe.error(new IllegalArgumentException("cannot request secret key, no key provided"));
        }
        return resolve(secretMount).flatMap(secretMap -> Maybe.fromOptional(secretMap.getSecret(secretMount)));
    }

    @Override
    public Flowable<SecretMap> watch(SecretMount secretMount, SecretEvent.Type... events) {
        final SecretProvider provider = secretProviders.getOrDefault(secretMount.provider(), new ErrorSecretProvider());
        return provider
            .watch(secretMount)
            .filter(secretEvent -> events == null || events.length == 0 || Arrays.asList(events).contains(secretEvent.type()))
            .subscribeOn(Schedulers.io())
            .map(SecretEvent::secretMap)
            .doFinally(provider::stop);
    }

    @Override
    public Flowable<Secret> watchKey(SecretMount secretMount, SecretEvent.Type... events) {
        if (secretMount.isKeyEmpty()) {
            return Flowable.error(new IllegalArgumentException("cannot request secret key, no key provided"));
        }
        return watch(secretMount, events).flatMapMaybe(secretMap -> Maybe.fromOptional(secretMap.getSecret(secretMount)));
    }

    public Optional<SecretProvider> findSecretProvider(String id) {
        return Optional.ofNullable(secretProviders.get(id));
    }

    public abstract boolean isEnabled(String pluginId);

    static class ErrorSecretProvider implements SecretProvider {

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
}
