package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.ssl;

import io.github.jopenlibs.vault.SslConfig;
import io.github.jopenlibs.vault.VaultException;
import io.gravitee.node.plugin.secretprovider.hcvault.HCVaultSecretProvider;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import io.gravitee.node.plugin.secretprovider.hcvault.util.EnumUtil;
import io.gravitee.node.secrets.api.util.ConfigHelper;
import java.io.File;
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
@Data
@NoArgsConstructor
@FieldNameConstants(level = AccessLevel.PACKAGE)
public class VaultSSLConfig {

    private boolean enabled;
    private Format format;
    private String file;
    private String pem;
    private String password;
    private SslConfig sslConfig;
    private VaultMTLSConfig mTLS;

    public VaultSSLConfig(Map<String, Object> properties) throws VaultException {
        final SslConfig ouc = new SslConfig();
        this.enabled = (boolean) properties.getOrDefault(Fields.enabled, false);
        if (enabled) {
            this.format =
                EnumUtil.valueOfCaseInsensitive(
                    "%s.%s.%s".formatted(HCVaultSecretProvider.PLUGIN_ID, VaultConfig.Fields.ssl, Fields.format),
                    (String) Objects.requireNonNull(properties.get(Fields.format)),
                    Format.class
                );
            switch (format) {
                case PEM -> {
                    pem = ConfigHelper.getStringOrSecret(properties, Fields.pem);
                    ouc.pemUTF8(pem);
                }
                case PEMFILE -> {
                    file = (String) Objects.requireNonNull(properties.get(Fields.file));
                    ouc.pemFile(new File(file));
                }
                case KEYSTORE -> {
                    file = (String) Objects.requireNonNull(properties.get(Fields.file));
                    password = ConfigHelper.getStringOrSecret(properties, Fields.password, null);
                    ouc.keyStoreFile(new File(file), password);
                }
                case TRUSTSTORE -> {
                    file = (String) Objects.requireNonNull(properties.get(Fields.file));
                    ouc.trustStoreFile(new File(file));
                }
            }
            mTLS = new VaultMTLSConfig(ConfigHelper.removePrefix(properties, Fields.mTLS));
            if (mTLS.isEnabled()) {
                if (mTLS.getFormat() == VaultMTLSConfig.Format.PEM) {
                    ouc.clientPemUTF8(mTLS.getCert());
                    ouc.clientKeyPemUTF8(mTLS.getKey());
                } else {
                    ouc.clientPemFile(new File(mTLS.getCert()));
                    ouc.clientPemFile(new File(mTLS.getKey()));
                }
            }
        }
        sslConfig = ouc.verify(enabled).build();
    }

    public enum Format {
        TRUSTSTORE,
        KEYSTORE,
        PEMFILE,
        PEM,
    }
}
