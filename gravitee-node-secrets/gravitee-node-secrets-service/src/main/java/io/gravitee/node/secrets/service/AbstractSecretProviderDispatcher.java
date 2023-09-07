package io.gravitee.node.secrets.service;

import io.gravitee.node.secrets.SecretProviderPlugin;
import io.gravitee.node.secrets.SecretProviderPluginManager;
import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.SecretProviderDispatcher;
import io.gravitee.node.secrets.api.SecretProviderFactory;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.node.secrets.api.model.*;
import io.gravitee.plugin.core.api.Plugin;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.*;
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
    private final Map<SecretLocation, SecretMap> secrets = Collections.synchronizedMap(new HashMap<>());

    protected AbstractSecretProviderDispatcher(SecretProviderPluginManager secretProviderPluginManager) {
        this.secretProviderPluginManager = secretProviderPluginManager;
    }

    protected final void createAndRegister(String id) {
        try {
            final SecretProviderPlugin<?, ?> secretProviderPlugin = secretProviderPluginManager.get(id);
            final Class<? extends SecretManagerConfiguration> configurationClass = secretProviderPlugin.configuration();
            final SecretProviderFactory<SecretManagerConfiguration> factory;
            if (id.equals("kubernetes")) {
                factory = secretProviderPluginManager.getFactoryById(id, true);
            } else {
                factory = secretProviderPluginManager.getFactoryById(id);
            }
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

    protected final void createAllSecretProviders() throws Exception {
        List<String> managers = secretProviderPluginManager.findAll().stream().map(Plugin::id).toList();

        // read all configs (in priority order if set)
        for (String id : managers) {
            if (isEnabled(id)) {
                createAndRegister(id);
            }
        }

        log.info("Secret provider loaded: {}", secretProviders.keySet());
    }

    public void stopAllProviders() {
        for (SecretProvider secretProvider : secretProviders.values()) {
            secretProvider.stop();
        }
    }

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException {
        if (secrets.containsKey(secretMount.location())) {
            return Maybe.just(secrets.get(secretMount.location()));
        }
        return secretProviders
            .getOrDefault(secretMount.provider(), new ErrorSecretProvider())
            .resolve(secretMount)
            .subscribeOn(Schedulers.io())
            .doOnSuccess(secretMap -> secrets.put(secretMount.location(), secretMap));
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
            .watch(secretMount, events)
            .subscribeOn(Schedulers.io())
            .doOnNext(event -> this.syncCache(event, secretMount.location())) // add cache call back so caller can know
            .filter(event -> event.type() != SecretEvent.Type.DELETED)
            .map(SecretEvent::secretMap)
            .doFinally(provider::stop);
    }

    @Override
    public Flowable<Secret> watchKey(SecretMount secretMount, SecretEvent.Type... events) {
        if (secretMount.isKeyEmpty()) {
            return Flowable.error(new IllegalArgumentException("cannot request secret key, no key provided"));
        }
        return watch(secretMount).flatMapMaybe(secretMap -> Maybe.fromOptional(secretMap.getSecret(secretMount)));
    }

    private void syncCache(SecretEvent secretEvent, SecretLocation secretLocation) {
        // NOSONAR TODO  use an actual cache and do it properly with TTL (or not based on the mount or plugin configuration)
        if (Objects.requireNonNull(secretEvent.type()) == SecretEvent.Type.DELETED) {
            secrets.remove(secretLocation);
        } else if (secretEvent.type() == SecretEvent.Type.CREATED || secretEvent.type() == SecretEvent.Type.UPDATED) {
            secrets.put(secretLocation, secretEvent.secretMap());
        }
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
        public Flowable<SecretEvent> watch(SecretMount secretMount, SecretEvent.Type... events) {
            return Flowable.error(new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(secretMount.provider())));
        }

        @Override
        public SecretMount fromURL(SecretURL url) {
            throw new SecretProviderNotFoundException("No secret provider plugin found for url: " + url);
        }
    }
}
