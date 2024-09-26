package io.gravitee.node.api.secrets.runtime.providers;

import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.core.Single;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ResolverService {
    Single<Entry> resolve(String envId, SecretMount secretMount);

    SecretMount toSecretMount(String envId, SecretURL secretURL);
}
