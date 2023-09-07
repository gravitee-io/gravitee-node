package io.gravitee.node.plugin.secretprovider.hcvault;

import io.gravitee.node.plugin.secretprovider.hcvault.client.VaultClient;
import io.gravitee.node.plugin.secretprovider.hcvault.client.auth.VaultAuthenticatorFactory;
import io.gravitee.node.plugin.secretprovider.hcvault.config.VaultSecretLocation;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.model.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

@Slf4j
public class HCVaultSecretProvider implements SecretProvider {

    private static final Map<String, SecretMap.WellKnownSecretKey> DEFAULT_WELL_KNOW_SECRET_KEYS = Map.of(
        "certificate",
        SecretMap.WellKnownSecretKey.CERTIFICATE,
        "private_key",
        SecretMap.WellKnownSecretKey.PRIVATE_KEY
    );
    public static final String PLUGIN_ID = "vault";
    private final VaultClient client;
    private final VaultConfig vaultConfig;

    public HCVaultSecretProvider(VaultConfig vaultConfig) throws SecretManagerException {
        VaultAuthenticatorFactory vaultAuthenticatorFactory = new VaultAuthenticatorFactory();
        this.client = new VaultClient(vaultConfig, vaultAuthenticatorFactory.create(vaultConfig.getAuth()));
        this.vaultConfig = vaultConfig;
    }

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        VaultSecretLocation location = VaultSecretLocation.fromLocation(secretMount.location());
        return client.read(location).doOnSuccess(secretMap -> handleWellKnownSecretKeys(secretMap, secretMount));
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
        if (url.path().indexOf(SecretURL.URL_SEPARATOR) < 0) {
            throw new SecretManagerConfigurationException(
                "URL is not valid for HC Vault Secret Provider plugin. Should be %s%s/<mount>/<secret>[:<data field>] but was: '%s'".formatted(
                        PLUGIN_URL_SCHEME,
                        PLUGIN_ID,
                        url
                    )
            );
        }
        VaultSecretLocation vaultSecretLocation = VaultSecretLocation.fromURL(url, vaultConfig);
        return new SecretMount(url.provider(), new SecretLocation(vaultSecretLocation.asMap()), vaultSecretLocation.key(), url);
    }

    // To Do this as the default behaviour (maybe at the dispatcher level)
    private void handleWellKnownSecretKeys(SecretMap secretMap, SecretMount secretMount) {
        secretMap.handleWellKnownSecretKeys(
            Optional
                .ofNullable(secretMount.secretURL())
                .map(SecretURL::wellKnowKeyMap)
                .filter(MapUtils::isNotEmpty)
                .orElse(DEFAULT_WELL_KNOW_SECRET_KEYS)
        );
    }
}
