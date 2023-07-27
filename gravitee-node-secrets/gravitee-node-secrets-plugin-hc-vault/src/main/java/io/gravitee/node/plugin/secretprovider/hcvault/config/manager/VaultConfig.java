package io.gravitee.node.plugin.secretprovider.hcvault.config.manager;

import io.github.jopenlibs.vault.VaultException;
import io.gravitee.node.plugin.secretprovider.hcvault.HCVaultSecretProvider;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultAppRoleAuthConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultAuthConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultGitHubAuthConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultTokenAuthConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth.VaultUserPassAuthConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.ssl.VaultSSLConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.util.EnumUtil;
import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.util.ConfigHelper;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@FieldNameConstants
public class VaultConfig implements SecretManagerConfiguration {

    private static final String DEPTH_2_CONFIG = "%s.%s";
    private static final String DEPTH_3_CONFIG = "%s.%s.%s";
    public static final String AUTH_CONFIG_FIELD = "config";

    private boolean enabled;
    private String host;
    private int port;
    private String nameSpace;
    private String token;
    private KVEngineVersion kvEngine;
    private int readTimeoutSec;
    private int connectTimeoutSec;
    private VaultRetryConfig retry;
    private VaultSSLConfig ssl;
    private VaultAuthConfig auth;
    private VaultWatchConfig watch;

    // called by introspection
    public VaultConfig(Map<String, Object> properties) throws VaultException {
        Objects.requireNonNull(properties);
        enabled = (boolean) properties.getOrDefault(Fields.enabled, false);
        host = (String) Objects.requireNonNull(properties.get(Fields.host));
        port = (int) Objects.requireNonNull(properties.get(Fields.port));
        nameSpace = (String) properties.getOrDefault(Fields.nameSpace, "default");
        kvEngine =
            EnumUtil.valueOfCaseInsensitive(
                DEPTH_2_CONFIG.formatted(HCVaultSecretProvider.PLUGIN_ID, Fields.kvEngine),
                (String) properties.getOrDefault(Fields.kvEngine, KVEngineVersion.V2.name()),
                KVEngineVersion.class
            );
        readTimeoutSec = (int) properties.getOrDefault(Fields.readTimeoutSec, 2);
        connectTimeoutSec = (int) properties.getOrDefault(Fields.connectTimeoutSec, 3);
        retry = new VaultRetryConfig(ConfigHelper.removePrefix(properties, Fields.retry));
        ssl = new VaultSSLConfig(ConfigHelper.removePrefix(properties, "ssl"));

        VaultAuthConfig.Method authMethod = EnumUtil.valueOfCaseInsensitive(
            DEPTH_3_CONFIG.formatted(HCVaultSecretProvider.PLUGIN_ID, Fields.auth, VaultAuthConfig.Fields.method),
            (String) Objects.requireNonNull(properties.get(DEPTH_2_CONFIG.formatted(Fields.auth, VaultAuthConfig.Fields.method))),
            VaultAuthConfig.Method.class
        );
        String authConfigPrefix = DEPTH_2_CONFIG.formatted(Fields.auth, AUTH_CONFIG_FIELD);
        switch (authMethod) {
            case TOKEN -> {
                auth = new VaultTokenAuthConfig(ConfigHelper.removePrefix(properties, authConfigPrefix));
                token = ((VaultTokenAuthConfig) auth).getToken();
            }
            case GITHUB -> auth = new VaultGitHubAuthConfig(ConfigHelper.removePrefix(properties, authConfigPrefix));
            case USERPASS -> auth = new VaultUserPassAuthConfig(ConfigHelper.removePrefix(properties, authConfigPrefix));
            case APPROLE -> auth = new VaultAppRoleAuthConfig(ConfigHelper.removePrefix(properties, authConfigPrefix));
        }
        watch = new VaultWatchConfig(ConfigHelper.removePrefix(properties, Fields.watch));
    }

    public io.github.jopenlibs.vault.VaultConfig toVaultConfig() throws VaultException {
        var config = new io.github.jopenlibs.vault.VaultConfig()
            .address(
                this.ssl.isEnabled() ? "https://%s:%d".formatted(this.host, this.port) : "http://%s:%d".formatted(this.host, this.port)
            )
            .nameSpace(this.nameSpace)
            .token(token)
            .engineVersion(kvEngine.version());
        config.sslConfig(this.ssl.getSslConfig());
        config.openTimeout(connectTimeoutSec);
        config.readTimeout(this.readTimeoutSec);

        return config.build();
    }

    public boolean isRetryEnabled() {
        return getRetry().getAttempts() > 0;
    }

    public VaultConfig withToken(String token) {
        this.token = token;
        return this;
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(fluent = true)
    public enum KVEngineVersion {
        V1(1),
        V2(2);

        private final int version;
    }
}
