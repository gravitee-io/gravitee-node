package com.graviteesource.services.runtimesecrets.providers;

import io.gravitee.node.api.secrets.runtime.providers.SecretProviderDeployer;
import java.util.Map;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FromConfigurationSecretProviderDeployer implements SecretProviderDeployer {

    private boolean init;
    private final Environment environment;

    public FromConfigurationSecretProviderDeployer(Environment environment) {
        this.environment = environment;
    }

    public void init() {
        if (!init) {
            doInit();
            init = true;
        }
    }

    private void doInit() {
        // TODO
    }

    @Override
    public void deploy(String id, String pluginId, Map<String, Object> config, String envId) {
        // TODO
    }
}
