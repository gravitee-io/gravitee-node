package io.gravitee.node.plugin.secretprovider.kubernetes;

import io.gravitee.node.plugin.secretprovider.kubernetes.config.KubernetesConfiguration;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.SecretProviderFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesSecretProviderFactory implements SecretProviderFactory<KubernetesConfiguration> {

    @Override
    public SecretProvider create(KubernetesConfiguration kubernetesConfiguration) {
        return new KubernetesSecretProvider(kubernetesConfiguration);
    }
}
