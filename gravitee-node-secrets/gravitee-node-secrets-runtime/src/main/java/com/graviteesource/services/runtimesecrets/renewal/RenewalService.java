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
package com.graviteesource.services.runtimesecrets.renewal;

import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.errors.SecretProviderException;
import com.graviteesource.services.runtimesecrets.spec.SpecUpdate;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.spec.Resolution;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.CacheKey;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class RenewalService {

    private final ResolverService resolverService;
    private final Cache cache;
    private final Config config;
    private Disposable poller;
    private final Map<Spec, Instant> specsToRenew = new ConcurrentHashMap<>();

    public void onSpec(Spec spec) {
        onSpec(new SpecUpdate(null, spec));
    }

    public void onSpec(SpecUpdate specUpdate) {
        Spec oldSpec = specUpdate.oldSpec();
        if (oldSpec != null) {
            specsToRenew.remove(oldSpec);
        }
        if (specUpdate.newSpec().hasResolutionType(Resolution.Type.POLL)) {
            setupNextCheck(specUpdate.newSpec());
        }
    }

    public void onDelete(Spec spec) {
        if (spec != null) {
            specsToRenew.remove(spec);
        }
    }

    public void start() {
        if (config.renewal().enabled()) {
            doStart();
        }
    }

    private void doStart() {
        poller =
            Flowable
                .<Instant, Instant>generate(
                    Instant::now,
                    (state, emitter) -> {
                        emitter.onNext(state);
                        return Instant.now();
                    }
                )
                .delay(config.renewal().duration().toMillis(), TimeUnit.MILLISECONDS)
                .rebatchRequests(1)
                .concatMap(now ->
                    Flowable
                        .fromIterable(specsToRenew.entrySet())
                        .filter(specAndTime -> now.isAfter(specAndTime.getValue()))
                        .map(Map.Entry::getKey)
                        .doOnNext(this::setupNextCheck)
                        .groupBy(CacheKey::from)
                        .flatMapSingle(group -> {
                            CacheKey cacheKey = group.getKey();
                            return resolverService
                                .toSecretMount(cacheKey.envId(), SecretURL.from(cacheKey.uri(), false))
                                .flatMap(secretMount -> resolverService.resolve(cacheKey.envId(), secretMount))
                                .flatMap(entry -> {
                                    if (entry.type() == Entry.Type.VALUE) {
                                        return group.collect(
                                            HashMap<String, Secret>::new,
                                            (map, spec) -> {
                                                String key = spec.key();
                                                map.put(key, entry.value().get(key));
                                            }
                                        );
                                    } else if (entry.type() == Entry.Type.EMPTY) {
                                        return Single.just(Map.<String, Secret>of());
                                    }
                                    return Single.error(new SecretProviderException(entry.error()));
                                })
                                .doOnError(err -> log.warn("Renewal failed for cache-key [{}]: {}", cacheKey, err.getMessage()))
                                .doOnSuccess(map -> cache.putPartial(cacheKey, map));
                        })
                )
                .subscribe();
    }

    public void stop() {
        if (poller != null) {
            poller.dispose();
        }
    }

    private void setupNextCheck(Spec spec) {
        specsToRenew.put(spec, Instant.now().plus(spec.resolution().duration()));
    }
}