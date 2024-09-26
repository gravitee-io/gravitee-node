package com.graviteesource.services.runtimesecrets.spec;

import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.spec.registry.EnvAwareSpecRegistry;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DefaultSpecLifecycleService implements SpecLifecycleService {

    private final EnvAwareSpecRegistry specRegistry;
    private final Cache cache;
    private final ResolverService resolverService;
    private final Config config;

    @Override
    public boolean shouldDeployOnTheFly(Ref ref) {
        return (ref.mainType() == Ref.MainType.URI && ref.mainExpression().isLiteral() && config.allowOnTheFlySpecs());
    }

    @Override
    public Spec deployOnTheFly(String envId, Ref ref) {
        Spec runtimeSpec = ref.toRuntimeSpec(envId);
        cache.computeIfAbsent(
            envId,
            runtimeSpec.naturalId(),
            () -> {
                specRegistry.register(envId, runtimeSpec);
                SecretURL secretURL = runtimeSpec.toSecretURL();
                SecretMount mount = resolverService.toSecretMount(envId, secretURL).withoutRetries();
                return resolverService
                    .resolve(envId, mount)
                    .subscribeOn(Schedulers.io())
                    .onErrorResumeNext(t -> {
                        Entry entry = new Entry(Entry.Type.ERROR, null, t.getMessage());
                        asyncResolution(runtimeSpec);
                        return Single.just(entry);
                    })
                    .blockingGet();
            }
        );

        return runtimeSpec;
    }

    private Disposable asyncResolution(Spec spec) {
        SecretURL secretURL = spec.toSecretURL();
        String envId = spec.envId();
        SecretMount mount = resolverService.toSecretMount(envId, secretURL);
        return resolverService
            .resolve(envId, mount)
            .subscribeOn(Schedulers.io())
            .onErrorResumeNext(t -> Single.just(new Entry(Entry.Type.ERROR, null, t.getMessage())))
            .subscribe(entry -> cache.put(envId, spec.naturalId(), entry));
    }

    @Override
    public void deploy(Spec spec) {
        specRegistry.register(spec.envId(), spec);
        // TODO check diff
        // TODO if change clean by old name or uri
        Disposable disposable = asyncResolution(spec/*, cleanupLambda*/);
    }

    @Override
    public void undeploy(Spec spec) {
        specRegistry.unregister(spec.envId(), spec);
        cache.evict(spec.envId(), spec.naturalId());
    }
}
