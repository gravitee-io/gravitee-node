package io.gravitee.node.plugin.secretprovider.hcvault.client.auth;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.AuthResponse;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultAuthConfig;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class VaultAuthenticator<C extends VaultAuthConfig> {

    @Getter
    private final C authConfig;

    private final AtomicReference<VaultToken> activeToken = new AtomicReference<>(null);
    private Disposable tokenRenewal;

    protected VaultAuthenticator(C authConfig) {
        this.authConfig = authConfig;
    }

    private VaultToken getActiveToken() {
        return activeToken.get();
    }

    private void updateActiveToken(VaultToken vaultToken) {
        this.activeToken.set(vaultToken);
    }

    public final Vault authenticate(VaultConfig vaultConfig) throws VaultException {
        if (getActiveToken() == null) {
            // first lookup
            VaultToken firstToken = doAuthenticate(newVaultFromConfig(vaultConfig));
            updateActiveToken(firstToken);
            if (firstToken.expiry() != null) {
                Duration delay = Duration.between(Instant.now(), firstToken.expiry());
                log.info("vault token will be renewed every {} for auth method: {}", delay, getAuthConfig().getMethod());
                // renew token when expiring, every expiring period
                this.tokenRenewal =
                    Schedulers
                        .io()
                        .schedulePeriodicallyDirect(
                            () -> {
                                try {
                                    VaultToken renewed = renewToken(newVaultWithToken(vaultConfig, getActiveToken()));
                                    log.info("vault token renewed for auth method: {}", getAuthConfig().getMethod());
                                    updateActiveToken(renewed);
                                } catch (VaultException e) {
                                    log.error("Cannot renew token", e);
                                }
                            },
                            delay.getSeconds(),
                            delay.getSeconds(),
                            TimeUnit.SECONDS
                        );
            }
        }
        return newVaultWithToken(vaultConfig, getActiveToken());
    }

    protected abstract VaultToken doAuthenticate(Vault vault) throws VaultException;

    protected VaultToken renewToken(Vault vault) throws VaultException {
        return doAuthenticate(vault);
    }

    public void stop() {
        if (tokenRenewal != null) {
            tokenRenewal.dispose();
        }
    }

    protected Vault newVaultWithToken(VaultConfig vaultConfig, VaultToken vaultToken) throws VaultException {
        return withRetry(
            Vault.create(vaultConfig.withToken(vaultToken.token()).toVaultConfig(), vaultConfig.getKvEngine().version()),
            vaultConfig
        );
    }

    protected Vault newVaultFromConfig(VaultConfig vaultConfig) throws VaultException {
        return withRetry(Vault.create(vaultConfig.toVaultConfig(), vaultConfig.getKvEngine().version()), vaultConfig);
    }

    private Vault withRetry(Vault cuc, VaultConfig vaultConfig) {
        if (vaultConfig.isRetryEnabled()) {
            cuc = cuc.withRetries(vaultConfig.getRetry().getAttempts(), vaultConfig.getRetry().getIntervalMs());
        }
        return cuc;
    }

    protected final VaultToken toVaultToken(AuthResponse authResponse) {
        String authClientToken = authResponse.getAuthClientToken();
        long authLeaseDuration = authResponse.getAuthLeaseDuration();
        if (authLeaseDuration > 0) {
            Instant expiry = Instant.now().plusSeconds(authLeaseDuration);
            return new VaultToken(authClientToken, expiry, authResponse.getRenewable());
        } else {
            return new VaultToken(authClientToken, null, authResponse.getRenewable());
        }
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(fluent = true)
    public enum Method {
        TOKEN,
        GITHUB,
        USERPASS,
        APPROLE,
    }
}
