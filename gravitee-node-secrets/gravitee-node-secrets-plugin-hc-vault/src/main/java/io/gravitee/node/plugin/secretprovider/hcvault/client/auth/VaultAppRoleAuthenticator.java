package io.gravitee.node.plugin.secretprovider.hcvault.client.auth;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultAppRoleAuthConfig;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VaultAppRoleAuthenticator extends VaultAuthenticator<VaultAppRoleAuthConfig> {

    protected VaultAppRoleAuthenticator(VaultAppRoleAuthConfig authConfig) {
        super(authConfig);
    }

    @Override
    protected VaultToken doAuthenticate(Vault vault) throws VaultException {
        return toVaultToken(vault.auth().loginByAppRole(getAuthConfig().getRoleId(), getAuthConfig().getSecretId()));
    }

    @Override
    protected VaultToken renewToken(Vault vault) throws VaultException {
        return doAuthenticate(vault);
    }
}
