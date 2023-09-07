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
public class VaultUserPassAuthConfig extends VaultAuthConfig {

    private String username;
    private String password;
    private String path;

    public VaultUserPassAuthConfig() {
        super(Method.USERPASS);
    }

    public VaultUserPassAuthConfig(Map<String, Object> properties) {
        super(Method.USERPASS);
        this.username = ConfigHelper.getStringOrSecret(properties, Fields.username);
        this.password = ConfigHelper.getStringOrSecret(properties, Fields.password);
        this.path = (String) properties.getOrDefault(Fields.path, "userpass");
    }
}
