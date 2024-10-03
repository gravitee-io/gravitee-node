package com.graviteesource.services.runtimesecrets;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.secrets.runtime.providers.SecretProviderDeployer;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class RuntimeSecretsService extends AbstractService<RuntimeSecretsService> {

    private final RuntimeSecretsProcessingService runtimeSecretsProcessingService;
    private final SpecLifecycleService specLifecycleService;
    private final SecretProviderDeployer secretProviderDeployer;

    @Override
    protected void doStart() throws Exception {
        secretProviderDeployer.init();
    }

    public void deploy(Spec spec) {
        specLifecycleService.deploy(spec);
    }

    public void undeploy(Spec spec) {
        specLifecycleService.undeploy(spec);
    }

    public <T> void onDefinitionDeploy(String envId, @Nonnull T definition, @Nullable Map<String, String> metadata) {
        runtimeSecretsProcessingService.onDefinitionDeploy(envId, definition, metadata);
    }
}
