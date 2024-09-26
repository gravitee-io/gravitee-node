package com.graviteesource.services.runtimesecrets.providers;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.core.Single;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultRuntimeResolver implements ResolverService {

    private final SecretProviderRegistry secretProviderRegistry;

    public DefaultRuntimeResolver(SecretProviderRegistry secretProviderRegistry) {
        this.secretProviderRegistry = secretProviderRegistry;
    }

    @Override
    public Single<Entry> resolve(String envId, SecretMount secretMount) {
        SecretProvider secretProvider = secretProviderRegistry.get(envId, secretMount.provider());
        return secretProvider
            .resolve(secretMount)
            .map(secretMap -> new Entry(Entry.Type.VALUE, secretMap.asMap(), null))
            .defaultIfEmpty(new Entry(Entry.Type.NOT_FOUND, null, null));
    }

    @Override
    public SecretMount toSecretMount(String envId, SecretURL secretURL) {
        return secretProviderRegistry.get(envId, secretURL.provider()).fromURL(secretURL);
    }
}
