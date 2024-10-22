/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graviteesource.services.runtimesecrets.providers;

import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.spec.Resolution;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Action;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultResolverService implements ResolverService {

    private final SecretProviderRegistry secretProviderRegistry;

    @Override
    public Single<Entry> resolve(String envId, SecretMount secretMount) {
        return resolve(envId, secretMount, new Resolution(Resolution.Type.ONCE, null));
    }

    public Single<Entry> resolve(String envId, SecretMount secretMount, Resolution resolution) {
        return secretProviderRegistry
            .get(envId, secretMount.provider())
            .flatMapMaybe(secretProvider -> secretProvider.resolve(secretMount))
            .map(secretMap -> this.applyExpiration(secretMap, resolution))
            .map(secretMap -> new Entry(Entry.Type.VALUE, secretMap.asMap(), null))
            .defaultIfEmpty(new Entry(Entry.Type.NOT_FOUND, null, null))
            .onErrorResumeNext(t -> Single.just(new Entry(Entry.Type.ERROR, null, t.getMessage())));
    }

    @Override
    public void resolveAsync(String envId, Spec spec, Duration delayBeforeResolve, @NonNull Action postResolution) {
        // TODO
    }

    private SecretMap applyExpiration(SecretMap secretMap, Resolution resolution) {
        if (resolution.type() == Resolution.Type.TTL) {
            // TODO move to Secret object
            secretMap.withExpireAt(Instant.now().plus(resolution.duration()));
        }
        return secretMap;
    }

    @Override
    public Single<SecretMount> toSecretMount(String envId, SecretURL secretURL) {
        return secretProviderRegistry.get(envId, secretURL.provider()).map(provider -> provider.fromURL(secretURL));
    }
}
