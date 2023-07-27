package io.gravitee.node.plugin.secretprovider.hcvault.client.auth;

import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VaultAuthenticatorFactory {

    @SuppressWarnings("unchecked")
    public <C extends VaultAuthConfig> VaultAuthenticator<C> create(C authConfig) {
        switch (authConfig.getMethod()) {
            case TOKEN -> {
                return (VaultAuthenticator<C>) new VaultTokenAuthenticator((VaultTokenAuthConfig) authConfig);
            }
            case GITHUB -> {
                return (VaultAuthenticator<C>) new VaultGitHubAuthenticator((VaultGitHubAuthConfig) authConfig);
            }
            case USERPASS -> {
                return (VaultAuthenticator<C>) new VaultUserPassAuthenticator((VaultUserPassAuthConfig) authConfig);
            }
            case APPROLE -> {
                return (VaultAuthenticator<C>) new VaultAppRoleAuthenticator((VaultAppRoleAuthConfig) authConfig);
            }
        }
        throw new IllegalArgumentException("vault auth method is not set");
    }
}
