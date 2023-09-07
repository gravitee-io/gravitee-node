package io.gravitee.node.api.secrets;

import io.gravitee.node.api.secrets.errors.SecretManagerException;
import io.gravitee.node.api.secrets.errors.SecretProviderNotFoundException;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.model.SecretEvent;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Interface to dispatch resolution "request" to the correct secret provider. It may cache secret internally.
 * This class is meant to have several implementation.
 *
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

    /**
     * Delegates to {@link SecretProvider#resolve(SecretMount)} in order to resolve a {@link SecretMap}.
     * Then uses {@link SecretMount#key()} to extract the secret from the map.
     * An empty maybe is returned if resolution returns no secret or the key do not exist in the secret map.
     *
     * @param secretMount the secret mount to resolve
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    Maybe<Secret> resolveKey(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException;

    /**
     * Delegates to {@link SecretProvider#watch(SecretMount, SecretEvent.Type...)} in order to resolve a {@link SecretMap}. Only created secret or updated secrets are returned.
     *
     * @param secretMount the secret mount to resolve
     * @param events      events to filter, null means "all"
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    Flowable<SecretMap> watch(SecretMount secretMount, SecretEvent.Type... events);

    /**
     * Delegates to {@link SecretProvider#watch(SecretMount, SecretEvent.Type...)} in order to resolve a {@link SecretMap}.
     * Then uses {@link SecretMount#key()} to extract the secret from the map.
     * No secret is published is none is found or the key do not exists in the secret map
     *
     * @param secretMount the secret mount to resolve
     * @param events      events to filter, null means "all"
     * @return a secret map
     * @throws SecretProviderNotFoundException if the {@link SecretMount#provider()} does match an enabled secret provider plugin
     * @throws SecretManagerException          if the secret manager throws an exception during resolution
     */
    Flowable<Secret> watchKey(SecretMount secretMount, SecretEvent.Type... events);
}
