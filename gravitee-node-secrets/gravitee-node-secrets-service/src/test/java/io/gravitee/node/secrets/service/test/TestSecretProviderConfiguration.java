package io.gravitee.node.secrets.service.test;

import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.util.ConfigHelper;
import java.util.Map;

public class TestSecretProviderConfiguration implements SecretManagerConfiguration {

    private final Map<String, Object> config;
    private final Map<String, Object> testSecrets;

    public TestSecretProviderConfiguration(Map<String, Object> config) {
        this.config = config;
        this.testSecrets = ConfigHelper.removePrefix(config, "secrets");
    }

    @Override
    public boolean isEnabled() {
        return (boolean) config.get("enabled");
    }

    public Map<String, ?> getTestSecrets() {
        return testSecrets;
    }
}
