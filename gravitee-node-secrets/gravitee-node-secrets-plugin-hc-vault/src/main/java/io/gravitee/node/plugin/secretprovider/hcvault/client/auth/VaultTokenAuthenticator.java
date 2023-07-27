package io.gravitee.node.plugin.secretprovider.hcvault.client.auth;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.LookupResponse;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultTokenAuthConfig;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class VaultTokenAuthenticator extends VaultAuthenticator<VaultTokenAuthConfig> {

    protected VaultTokenAuthenticator(VaultTokenAuthConfig authConfig) {
        super(authConfig);
    }

    @Override
    protected VaultToken doAuthenticate(Vault vault) throws VaultException {
        return toActiveToken(vault.auth().lookupSelf());
    }

    @Override
    protected VaultToken renewToken(Vault vault) throws VaultException {
        return toVaultToken(vault.auth().renewSelf());
    }

    private VaultToken toActiveToken(LookupResponse response) {
        return new VaultToken(getAuthConfig().getToken(), Instant.now().plusSeconds(response.getTTL()), response.isRenewable());
    }
}
