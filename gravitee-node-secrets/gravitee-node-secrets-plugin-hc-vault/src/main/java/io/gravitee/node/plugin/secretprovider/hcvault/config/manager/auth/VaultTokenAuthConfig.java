package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth;

import java.util.Map;
import java.util.Objects;
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
public class VaultTokenAuthConfig extends VaultAuthConfig {

    private String token;

    public VaultTokenAuthConfig() {
        super(Method.TOKEN);
    }

    public VaultTokenAuthConfig(Map<String, Object> properties) {
        super(Method.TOKEN);
        token = (String) Objects.requireNonNull(properties.get(VaultGitHubAuthConfig.Fields.token));
    }
}
