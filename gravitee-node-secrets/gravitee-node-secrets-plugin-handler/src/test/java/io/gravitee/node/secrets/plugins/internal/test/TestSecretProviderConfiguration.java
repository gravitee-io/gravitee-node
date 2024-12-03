package io.gravitee.node.secrets.plugins.internal.test;

import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;

public class TestSecretProviderConfiguration implements SecretManagerConfiguration {

    @Override
    public boolean isEnabled() {
        return true;
    }
}
