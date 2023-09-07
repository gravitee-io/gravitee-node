package io.gravitee.node.plugin.secretprovider.hcvault.client;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.gravitee.node.api.secrets.model.SecretEvent;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.plugin.secretprovider.hcvault.client.auth.VaultAuthenticator;
import io.gravitee.node.plugin.secretprovider.hcvault.config.VaultSecretLocation;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VaultClient {

    private final VaultConfig vaultConfig;
    private final VaultAuthenticator<?> authenticator;
    private final Map<VaultSecretLocation, Flowable<SecretEvent>> pollers = new HashMap<>();

    public VaultClient(VaultConfig vaultConfig, VaultAuthenticator<?> authenticator) {
        this.vaultConfig = vaultConfig;
        this.authenticator = authenticator;
    }

    public Maybe<SecretMap> read(VaultSecretLocation location) {
        try {
            Vault vault = authenticator.authenticate(vaultConfig);
            String namespace = location.namespace();
            if (namespace == null || namespace.isBlank()) {
                return Maybe.just(vault.logical().read(location.secretPath())).flatMap(VaultClient::toSecret);
            } else {
                return Maybe.just(vault.logical().withNameSpace(namespace).read(location.secretPath())).flatMap(VaultClient::toSecret);
            }
        } catch (VaultException e) {
            return Maybe.error(e);
        }
    }

    public Flowable<SecretEvent> poll(VaultSecretLocation location) {
        if (!vaultConfig.getWatch().isEnabled()) {
            return Flowable.error(new IllegalStateException("vault watch is disabled"));
        }
        return pollers.computeIfAbsent(
            location,
            loc ->
                Flowable
                    .interval(vaultConfig.getWatch().getPollIntervalSec(), TimeUnit.SECONDS)
                    .flatMapMaybe(i -> this.read(loc))
                    // ideally if the vault-java-driver would return the version ???
                    .map(secret -> new SecretEvent(SecretEvent.Type.UPDATED, secret))
                    .share()
                    .onErrorResumeNext(e -> {
                        if (e instanceof VaultException va && va.getHttpStatusCode() == 404) {
                            return Flowable.just(new SecretEvent(SecretEvent.Type.DELETED, new SecretMap(null)));
                        }
                        return Flowable.error(e);
                    })
                    .doFinally(() -> pollers.remove(loc))
        );
    }

    public void stop() {
        authenticator.stop();
    }

    public static Maybe<SecretMap> toSecret(LogicalResponse response) {
        Instant expireAt = null;
        if (response.getLeaseDuration() > 0) {
            expireAt = Instant.now().plusSeconds(response.getLeaseDuration());
        }
        return Maybe.just(SecretMap.of(response.getData(), expireAt));
    }
}
