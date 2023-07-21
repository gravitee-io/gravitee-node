package io.gravitee.node.plugin.secretprovider.kubernetes;

import io.gravitee.node.plugin.secretprovider.kubernetes.config.K8sConfig;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.SecretProviderFactory;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesSecretProviderFactory implements SecretProviderFactory<K8sConfig> {

    @Override
    public SecretProvider create(K8sConfig kubernetesConfiguration) {
        return new KubernetesSecretProvider(kubernetesConfiguration);
    }
}
