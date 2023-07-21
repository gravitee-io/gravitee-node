package io.gravitee.node.secrets.api;

import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.model.SecretEvent;
import io.gravitee.node.secrets.api.model.SecretMount;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProviderDispatcher {
    <T extends SecretManagerConfiguration> T readConfiguration(String managerId, Class<?> configurationClass);

    Maybe<Secret> resolve(SecretMount secretMount) throws SecretProviderNotFoundException, SecretManagerException;

    Flowable<Secret> watch(SecretMount secretMount, SecretEvent.Type... events);
}
