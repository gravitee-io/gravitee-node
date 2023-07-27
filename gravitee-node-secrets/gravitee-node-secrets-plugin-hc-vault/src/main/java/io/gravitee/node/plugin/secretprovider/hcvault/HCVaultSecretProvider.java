package io.gravitee.node.plugin.secretprovider.hcvault;

import io.gravitee.node.plugin.secretprovider.hcvault.client.VaultClient;
import io.gravitee.node.plugin.secretprovider.hcvault.client.auth.VaultAuthenticatorFactory;
import io.gravitee.node.plugin.secretprovider.hcvault.config.VaultSecretLocation;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.model.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HCVaultSecretProvider implements SecretProvider {

    public static final String PLUGIN_ID = "vault";
    private final VaultClient client;

    public HCVaultSecretProvider(VaultConfig vaultConfig) throws SecretManagerException {
        VaultAuthenticatorFactory vaultAuthenticatorFactory = new VaultAuthenticatorFactory();
        this.client = new VaultClient(vaultConfig, vaultAuthenticatorFactory.create(vaultConfig.getAuth()));
    }

    @Override
    public Maybe<Secret> resolve(SecretMount secretMount) {
        VaultSecretLocation location = VaultSecretLocation.fromLocation(secretMount.location());
        return client.read(location);
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount, SecretEvent.Type... types) {
        VaultSecretLocation location = VaultSecretLocation.fromLocation(secretMount.location());
        return client.poll(location);
    }

    @Override
    public SecretProvider stop() {
        client.stop();
        return this;
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        VaultSecretLocation vaultSecretLocation = VaultSecretLocation.fromURL(url);
        return new SecretMount(url.provider(), new SecretLocation(vaultSecretLocation.asMap()), url);
    }
}
