package io.gravitee.node.plugin.secretprovider.hcvault.client.auth;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultUserPassAuthConfig;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VaultUserPassAuthenticator extends VaultAuthenticator<VaultUserPassAuthConfig> {

    protected VaultUserPassAuthenticator(VaultUserPassAuthConfig authConfig) {
        super(authConfig);
    }

    @Override
    protected VaultToken doAuthenticate(Vault vault) throws VaultException {
        return toVaultToken(
            vault.auth().loginByUserPass(getAuthConfig().getUsername(), getAuthConfig().getPassword(), getAuthConfig().getPath())
        );
    }
}
