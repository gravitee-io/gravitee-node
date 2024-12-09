package io.gravitee.node.secrets.config.test;

import io.gravitee.secrets.api.plugin.SecretProvider;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TestSecretProviderFactory implements SecretProviderFactory<TestSecretProviderConfiguration> {

    @Override
    public SecretProvider create(TestSecretProviderConfiguration configuration) {
        return new TestSecretProvider(configuration);
    }
}
