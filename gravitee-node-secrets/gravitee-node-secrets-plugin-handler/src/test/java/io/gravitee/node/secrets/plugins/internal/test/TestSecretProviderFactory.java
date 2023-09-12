package io.gravitee.node.secrets.plugins.internal.test;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.SecretProviderFactory;

public class TestSecretProviderFactory implements SecretProviderFactory<TestSecretProviderConfiguration> {

    public TestSecretProviderFactory() {}

    @Override
    public SecretProvider create(TestSecretProviderConfiguration configuration) {
        return new TestSecretProvider();
    }
}
