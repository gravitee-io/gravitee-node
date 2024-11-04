package io.gravitee.node.api.secrets.runtime.providers;

import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Action;
import java.time.Duration;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ResolverService {
    Single<Entry> resolve(String envId, SecretMount secretMount);

    void resolveAsync(Spec spec, Duration delayBeforeResolve, boolean retryOnError, @NonNull Action postResolution);

    Single<SecretMount> toSecretMount(String envId, SecretURL secretURL);
}
