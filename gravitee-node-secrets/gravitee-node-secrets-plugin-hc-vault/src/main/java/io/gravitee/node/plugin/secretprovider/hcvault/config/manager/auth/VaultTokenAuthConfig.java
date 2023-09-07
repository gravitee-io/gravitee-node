package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth;

import io.gravitee.node.api.secrets.util.ConfigHelper;
import java.util.Map;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(level = AccessLevel.PACKAGE)
public class VaultTokenAuthConfig extends VaultAuthConfig {

    private String token;

    public VaultTokenAuthConfig() {
        super(Method.TOKEN);
    }

    public VaultTokenAuthConfig(Map<String, Object> properties) {
        super(Method.TOKEN);
        token = ConfigHelper.getStringOrSecret(properties, VaultGitHubAuthConfig.Fields.token);
    }
}
