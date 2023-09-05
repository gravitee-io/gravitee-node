package io.gravitee.node.secrets.internal.fake;

import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.SecretProviderFactory;

public class TestSecretProviderFactory implements SecretProviderFactory<TestSecretProviderConfiguration> {

    public TestSecretProviderFactory() {}

    @Override
    public SecretProvider create(TestSecretProviderConfiguration configuration) {
        return new TestSecretProvider();
    }
}
