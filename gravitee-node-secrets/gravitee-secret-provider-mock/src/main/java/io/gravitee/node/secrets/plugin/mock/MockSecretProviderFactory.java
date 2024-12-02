package io.gravitee.node.secrets.plugin.mock;

import io.gravitee.node.secrets.plugin.mock.conf.MockSecretProviderConfiguration;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;

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
