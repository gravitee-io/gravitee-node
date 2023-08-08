package io.gravitee.node.secrets.api;

import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.model.SecretEvent;
import io.gravitee.node.secrets.api.model.SecretMap;
import io.gravitee.node.secrets.api.model.SecretMount;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProviderDispatcher {
    /**
     * Read the plugins configuration
     *
     * @param pluginId           the plugin id
     * @param configurationClass the class to return
     * @param <T>                the configuration type extending {@link SecretManagerConfiguration}
     * @return the configuration class
     */
    <T extends SecretManagerConfiguration> T readConfiguration(String pluginId, Class<?> configurationClass);

    /**
     * Delegates to {@link SecretProvider#resolve(SecretMount)} in order to resolve a {@link SecretMap}
     *
     * @param secretMount the secret mount to resolve
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    Maybe<SecretMap> resolve(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException;

    Maybe<Secret> resolveKey(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException;

    Flowable<SecretMap> watch(SecretMount secretMount, SecretEvent.Type... events);

    Flowable<Secret> watchKey(SecretMount secretMount, SecretEvent.Type... events);
}
