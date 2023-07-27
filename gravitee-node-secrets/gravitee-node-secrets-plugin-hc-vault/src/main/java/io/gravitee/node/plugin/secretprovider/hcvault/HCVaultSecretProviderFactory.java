package io.gravitee.node.plugin.secretprovider.hcvault;

import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.SecretProviderFactory;

public class HCVaultSecretProviderFactory implements SecretProviderFactory<VaultConfig> {

    @Override
    public SecretProvider create(VaultConfig vaultConfig) {
        return new HCVaultSecretProvider(vaultConfig);
    }
}
