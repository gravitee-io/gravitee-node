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
package com.graviteesource.services.runtimesecrets.spec;

import com.graviteesource.services.runtimesecrets.config.Config;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.discovery.ContextRegistry;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultSpecLifecycleService implements SpecLifecycleService {

    private final SpecRegistry specRegistry;
    private final ContextRegistry contextRegistry;
    private final Cache cache;
    private final ResolverService resolverService;
    private final GrantService grantService;
    private final Config config;

    @Override
    public boolean shouldDeployOnTheFly(Ref ref) {
        return (ref.mainType() == Ref.MainType.URI && ref.mainExpression().isLiteral() && config.onTheFlySpecsEnabled());
    }

    @Override
    public Spec deployOnTheFly(String envId, Ref ref) {
        Spec runtimeSpec = ref.asOnTheFlySpec(envId);
        cache.computeIfAbsent(
            envId,
            runtimeSpec.naturalId(),
            () -> {
                specRegistry.register(runtimeSpec);
                SecretURL secretURL = runtimeSpec.toSecretURL();
                return resolverService
                    .toSecretMount(envId, secretURL)
                    .map(SecretMount::withoutRetries)
                    .flatMap(mount ->
                        resolverService
                            .resolve(envId, mount)
                            .doOnSuccess(entry -> {
                                if (entry.type() == Entry.Type.ERROR) {
                                    asyncResolution(runtimeSpec, config.onTheFlySpecsDelayBeforeRetryMs(), () -> {});
                                }
                            })
                            .subscribeOn(Schedulers.io())
                    )
                    .blockingGet();
            }
        );

        return runtimeSpec;
    }

    @Override
    public void deploy(Spec newSpec) {
        Spec currentSpec = specRegistry.fromSpec(newSpec.envId(), newSpec);
        log.info("Deploying Secret Spec: {}", newSpec);
        Action afterResolve = () -> {
            specRegistry.register(newSpec);
        };
        boolean shouldResolve = true;
        if (currentSpec != null) {
            if (isNameOrLocationChanged(currentSpec, newSpec)) {
                afterResolve =
                    () -> {
                        renewGrant(currentSpec, newSpec);
                        specRegistry.replace(currentSpec, newSpec);
                        if (!currentSpec.naturalId().equals(newSpec.naturalId())) {
                            cache.evict(newSpec.envId(), currentSpec.naturalId());
                        }
                    };
            } else if (isACLsChange(newSpec, currentSpec)) {
                renewGrant(currentSpec, newSpec);
                shouldResolve = false;
            }
        }

        if (shouldResolve) {
            asyncResolution(newSpec, 0, afterResolve);
        }
    }

    private void renewGrant(Spec oldSpec, Spec newSpec) {
        contextRegistry
            .findBySpec(oldSpec)
            .forEach(context -> {
                if (grantService.isGranted(context, newSpec)) {
                    grantService.grant(context, newSpec);
                } else {
                    grantService.revoke(context);
                }
            });
        specRegistry.replace(oldSpec, newSpec);
    }

    private static boolean isACLsChange(Spec spec, Spec previousSpec) {
        return !Objects.equals(previousSpec.acls(), spec.acls());
    }

    private boolean isNameOrLocationChanged(Spec oldSpec, Spec newSpec) {
        record LiteSpec(String name, String uriAndKey) {}
        return !Objects.equals(new LiteSpec(oldSpec.name(), oldSpec.uriAndKey()), new LiteSpec(newSpec.name(), newSpec.uriAndKey()));
    }

    @Override
    public void undeploy(Spec spec) {
        contextRegistry.findBySpec(spec).forEach(grantService::revoke);
        cache.evict(spec.envId(), spec.naturalId());
        specRegistry.unregister(spec);
    }

    private void asyncResolution(Spec spec, long delayMs, @NonNull Action postResolution) {
        SecretURL secretURL = spec.toSecretURL();
        String envId = spec.envId();
        resolverService
            .toSecretMount(envId, secretURL)
            .delay(delayMs, TimeUnit.MILLISECONDS)
            .doOnSuccess(mount -> {
                log.info("Resolving secret: {}", mount);
            })
            .flatMap(mount -> resolverService.resolve(envId, mount).subscribeOn(Schedulers.io()))
            .doOnTerminate(postResolution)
            .subscribe(
                new SimpleSingleObserver<>() {
                    @Override
                    public void onSuccess(@NonNull Entry entry) {
                        cache.put(spec.envId(), spec.naturalId(), entry);
                    }

                    @Override
                    public void onError(@NonNull Throwable err) {
                        log.error("Async resolution failed", err);
                    }
                }
            );
    }

    private abstract static class SimpleSingleObserver<T> implements SingleObserver<T> {

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            // no op
        }
    }
}
