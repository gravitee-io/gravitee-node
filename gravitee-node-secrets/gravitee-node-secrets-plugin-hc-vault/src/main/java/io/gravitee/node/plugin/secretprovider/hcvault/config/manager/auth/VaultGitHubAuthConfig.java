package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth;

import io.gravitee.node.api.secrets.util.ConfigHelper;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(level = AccessLevel.PACKAGE)
public class VaultGitHubAuthConfig extends VaultAuthConfig {

    private String token;
    private String path;

    public VaultGitHubAuthConfig(Map<String, Object> properties) {
        super(Method.GITHUB);
        token = ConfigHelper.getStringOrSecret(properties, Fields.token);
        path = (String) properties.getOrDefault(Fields.path, "github");
    }
}
