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

import static java.util.function.Predicate.not;

import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.renewal.RenewalService;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.discovery.ContextRegistry;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.CacheKey;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.functions.Action;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
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
    private final RenewalService renewalService;
    private final Config config;

    @Override
    public boolean shouldDeployOnTheFly(Ref ref) {
        return (ref.mainType() == Ref.MainType.URI && ref.mainExpression().isLiteral() && config.onTheFlySpecs().enabled());
    }

    @Override
    public Spec deployOnTheFly(String envId, Ref ref) {
        Spec runtimeSpec = ref.asOnTheFlySpec(envId);
        specRegistry.register(runtimeSpec);
        cache.computeIfAbsent(
            CacheKey.from(runtimeSpec),
            () -> {
                SecretURL secretURL = runtimeSpec.toSecretURL();
                return resolverService
                    .toSecretMount(envId, secretURL)
                    .doOnSuccess(mount -> log.info("Resolving secret (On the fly): {}", mount))
                    .map(SecretMount::withoutRetries)
                    .flatMap(mount ->
                        resolverService
                            .resolve(envId, mount)
                            .doOnSuccess(entry -> {
                                if (entry.type() == Entry.Type.ERROR) {
                                    asyncResolution(runtimeSpec, config.onTheFlySpecs().onErrorRetryAfter(), () -> {});
                                }
                            })
                    )
                    .blockingGet();
            }
        );

        return runtimeSpec;
    }

    @Override
    public void deploy(Spec spec) {
        Spec currentSpec = specRegistry.fromSpec(spec);
        log.info("Deploying Secret Spec: {}", spec);
        Action afterResolve = () -> specRegistry.register(spec);
        boolean shouldResolve = true;
        if (currentSpec != null) {
            SpecUpdate update = new SpecUpdate(currentSpec, spec);
            if (isUriAndKeyChanged(update)) {
                afterResolve =
                    () -> {
                        renewGrant(update);
                        specRegistry.replace(update);
                        if (!Objects.equals(CacheKey.from(update.oldSpec()), CacheKey.from(update.newSpec()))) {
                            cache.evict(CacheKey.from(update.oldSpec()));
                        }
                    };
            } else if (isACLsChange(update)) {
                renewGrant(update);
                specRegistry.replace(update);
                shouldResolve = false;
            }
            renewalService.onSpec(update);
        } else {
            renewalService.onSpec(spec);
            contextRegistry.findBySpec(spec).forEach(context -> grantService.grant(context, spec));
        }

        if (shouldResolve) {
            asyncResolution(spec, Duration.ZERO, afterResolve);
        }
    }

    private void renewGrant(SpecUpdate update) {
        contextRegistry
            .findBySpec(update.oldSpec())
            .forEach(context -> {
                boolean grant = grantService.grant(context, update.newSpec());
                if (!grant) {
                    grantService.revoke(context);
                }
            });
    }

    private static boolean isACLsChange(SpecUpdate specUpdate) {
        Predicate<SpecUpdate> sameACLs = update -> Objects.equals(update.oldSpec().acls(), update.newSpec().acls());
        Predicate<SpecUpdate> sameKind = update -> update.oldSpec().valueKind() == update.newSpec().valueKind();
        return not(sameACLs).or(not(sameKind)).test(specUpdate);
    }

    private boolean isUriAndKeyChanged(SpecUpdate update) {
        return !Objects.equals(update.oldSpec().uriAndKey(), update.newSpec().uriAndKey());
    }

    @Override
    public void undeploy(Spec spec) {
        contextRegistry.findBySpec(spec).forEach(grantService::revoke);
        specRegistry.unregister(spec);
        cache.evict(CacheKey.from(spec));
        renewalService.onDelete(spec);
    }

    private void asyncResolution(Spec spec, Duration delay, @NonNull Action postResolution) {
        SecretURL secretURL = spec.toSecretURL();
        String envId = spec.envId();
        resolverService
            .toSecretMount(envId, secretURL)
            .delay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .doOnSuccess(mount -> log.info("Resolving secret: {}", mount))
            .flatMap(mount -> resolverService.resolve(envId, mount))
            .doOnError(err -> log.error("Async resolution failed", err))
            .doFinally(postResolution)
            .subscribe(entry -> cache.put(CacheKey.from(spec), entry));
    }
}
