package com.graviteesource.services.runtimesecrets.el;

import com.graviteesource.services.runtimesecrets.spec.registry.EnvAwareSpecRegistry;
import io.gravitee.el.TemplateContext;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ContextUpdater {

    private final Cache cache;
    private final GrantService grantService;
    private final SpecLifecycleService specLifecycleService;
    private final EnvAwareSpecRegistry specRegistry;

    public void addRuntimeSecretsService(TemplateContext context) {
        context.setVariable("secrets", new Service(cache, grantService, specLifecycleService, specRegistry));
    }
}
