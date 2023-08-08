package io.gravitee.node.plugin.secretprovider.aws;

import io.gravitee.node.plugin.secretprovider.aws.config.AWSConfig;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.SecretProviderFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AWSSecretProviderFactory implements SecretProviderFactory<AWSConfig> {

    @Override
    public SecretProvider create(AWSConfig configuration) {
        return new AWSSecretProvider(configuration);
    }
}
