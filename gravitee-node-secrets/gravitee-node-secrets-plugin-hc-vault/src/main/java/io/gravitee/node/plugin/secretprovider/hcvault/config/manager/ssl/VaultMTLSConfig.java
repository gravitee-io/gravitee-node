package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.ssl;

import io.gravitee.node.plugin.secretprovider.hcvault.HCVaultSecretProvider;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.util.EnumUtil;
import io.gravitee.node.secrets.api.util.ConfigHelper;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@FieldNameConstants(level = AccessLevel.PACKAGE)
@Data
@NoArgsConstructor
public class VaultMTLSConfig {

    private boolean enabled;
    private Format format;
    private String cert;
    private String key;

    public VaultMTLSConfig(Map<String, Object> properties) {
        this.enabled = (boolean) properties.getOrDefault(Fields.enabled, false);
        this.format =
            EnumUtil.valueOfCaseInsensitive(
                "%s.%s.%s".formatted(HCVaultSecretProvider.PLUGIN_ID, VaultConfig.Fields.ssl, Fields.format),
                (String) Objects.requireNonNull(properties.get(Fields.format)),
                Format.class
            );
        this.cert = ConfigHelper.getStringOrSecret(properties, Fields.cert);
        this.key = ConfigHelper.getStringOrSecret(properties, Fields.key);
    }

    public enum Format {
        PEM,
        PEMFILE,
    }
}
