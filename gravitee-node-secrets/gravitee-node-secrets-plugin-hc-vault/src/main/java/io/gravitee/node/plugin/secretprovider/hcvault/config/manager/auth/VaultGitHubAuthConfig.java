package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
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
public class VaultGitHubAuthConfig extends VaultAuthConfig {

    private String token;
    private String path;

    public VaultGitHubAuthConfig(Map<String, Object> properties) {
        super(Method.GITHUB);
        token = (String) Objects.requireNonNull(properties.get(Fields.token));
        path = (String) properties.getOrDefault(Fields.path, "github");
    }
}
