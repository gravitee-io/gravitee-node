package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.sys.mounts.TimeToLive;
import java.util.Map;
import java.util.Objects;
import lombok.*;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@FieldNameConstants(level = AccessLevel.PACKAGE)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class VaultAppRoleAuthConfig extends VaultAuthConfig {

    private String roleId;
    private String secretId;
    private TimeToLive secretIdTTL;

    public VaultAppRoleAuthConfig() {
        super(Method.APPROLE);
    }

    public VaultAppRoleAuthConfig(Map<String, Object> properties) {
        super(Method.APPROLE);
        roleId = (String) Objects.requireNonNull(properties.get(Fields.roleId));
        secretId = (String) Objects.requireNonNull(properties.get(Fields.secretId));
    }
}
