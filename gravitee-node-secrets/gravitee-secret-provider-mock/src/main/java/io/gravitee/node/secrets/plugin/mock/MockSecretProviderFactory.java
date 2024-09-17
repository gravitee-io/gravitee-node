package io.gravitee.node.secrets.plugin.mock;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.SecretProviderFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MockSecretProviderFactory implements SecretProviderFactory<MockSecretProviderConfiguration> {

    @Override
    public SecretProvider create(MockSecretProviderConfiguration configuration) {
        return new MockSecretProvider(configuration);
    }
}
