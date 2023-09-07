package io.gravitee.node.secrets.internal.fake;

import io.gravitee.node.api.secrets.SecretManagerConfiguration;

public class TestSecretProviderConfiguration implements SecretManagerConfiguration {

    @Override
    public boolean isEnabled() {
        return true;
    }
}
