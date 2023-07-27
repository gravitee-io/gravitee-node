package io.gravitee.node.plugin.secretprovider.hcvault.client.auth;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultGitHubAuthConfig;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VaultGitHubAuthenticator extends VaultAuthenticator<VaultGitHubAuthConfig> {

    public VaultGitHubAuthenticator(VaultGitHubAuthConfig authConfig) {
        super(authConfig);
    }

    @Override
    protected VaultToken doAuthenticate(Vault vault) throws VaultException {
        return toVaultToken(vault.auth().loginByGithub(getAuthConfig().getToken(), getAuthConfig().getPath()));
    }
}
