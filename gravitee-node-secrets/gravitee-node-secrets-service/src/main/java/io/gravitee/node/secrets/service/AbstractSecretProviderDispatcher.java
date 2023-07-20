package io.gravitee.node.secrets.service;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.secrets.SecretProviderPlugin;
import io.gravitee.node.secrets.SecretProviderPluginManager;
import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.SecretProviderDispatcher;
import io.gravitee.node.secrets.api.SecretProviderFactory;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.model.SecretEvent;
import io.gravitee.node.secrets.api.model.SecretMount;
import io.gravitee.plugin.core.api.Plugin;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
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
    private final Map<SecretMount, Secret> secrets = Collections.synchronizedMap(new HashMap<>());

    protected AbstractSecretProviderDispatcher(SecretProviderPluginManager secretProviderPluginManager) {
        this.secretProviderPluginManager = secretProviderPluginManager;
    }

    protected void init() throws Exception {
        // enabled managers
        List<String> enabledManagers =
            this.filterEnabledManagers(secretProviderPluginManager.findAll().stream().map(Plugin::id).collect(Collectors.toSet()));

        // read all configs (in priority order if set)
        for (String id : enabledManagers) {
            SecretProviderPlugin<?, ?> secretProviderPlugin = secretProviderPluginManager.get(id);
            Class<? extends SecretManagerConfiguration> configurationClass = secretProviderPlugin.configuration();
            SecretProviderFactory<SecretManagerConfiguration> factory = secretProviderPluginManager.getFactoryById(id);
            if (configurationClass != null && factory != null) {
                // read the config using the plugin class loader
                SecretManagerConfiguration config =
                    this.readConfiguration(id, factory.getClass().getClassLoader().loadClass(configurationClass.getName()));
                // register and start
                secretProviders.put(id, factory.create(config).start());
            } else {
                throw new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(id));
            }
        }

        log.info("Secret provider loaded: {}", enabledManagers);
    }

    protected void stop() throws Exception {
        for (SecretProvider secretProvider : secretProviders.values()) {
            secretProvider.stop();
        }
    }

    @Override
    public Maybe<Secret> resolve(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException {
        if (secrets.containsKey(secretMount)) {
            return Maybe.just(secrets.get(secretMount));
        }
        return secretProviders
            .getOrDefault(secretMount.provider(), new ErrorSecretProvider())
            .resolve(secretMount)
            .doOnSuccess(secret -> secrets.put(secretMount, secret));
    }

    @Override
    public Flowable<Secret> watch(SecretMount secretMount, SecretEvent.Type... events) {
        final SecretProvider provider = secretProviders.getOrDefault(secretMount.provider(), new ErrorSecretProvider());
        return provider
            .watch(secretMount, events)
            .doOnNext(event -> this.syncCache(event, secretMount)) // add cache call back so caller can know
            .map(SecretEvent::secret)
            .doFinally(provider::stop);
    }

    private void syncCache(SecretEvent secretEvent, SecretMount secretMount) {
        //NOSONAR TODO  use an actual cache and do it properly with TTL (or not based on the mount or plugin configuration)
        if (Objects.requireNonNull(secretEvent.type()) == SecretEvent.Type.DELETED) {
            secrets.remove(secretMount);
        } else if (secretEvent.type() == SecretEvent.Type.CREATED || secretEvent.type() == SecretEvent.Type.UPDATED) {
            secrets.put(secretMount, secretEvent.secret());
        }
    }

    public Optional<SecretProvider> findSecretProvider(String id) {
        return Optional.ofNullable(secretProviders.get(id));
    }

    static class ErrorSecretProvider implements SecretProvider {

        @Override
        public Maybe<Secret> resolve(SecretMount secretMount) {
            return Maybe.error(new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(secretMount.provider())));
        }

        @Override
        public Flowable<SecretEvent> watch(SecretMount secretMount, SecretEvent.Type... events) {
            return Flowable.error(new SecretProviderNotFoundException(SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(secretMount.provider())));
        }

        @Override
        public SecretMount fromURL(URL url) {
            throw new SecretProviderNotFoundException("No secret provider plugin found for url: " + url);
        }
    }
}
