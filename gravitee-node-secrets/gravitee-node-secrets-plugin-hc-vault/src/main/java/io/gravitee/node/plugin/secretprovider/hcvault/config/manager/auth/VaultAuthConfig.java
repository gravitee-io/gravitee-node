package io.gravitee.node.plugin.secretprovider.hcvault.config.manager.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@FieldNameConstants
public abstract class VaultAuthConfig {

    @Getter
    private Method method;

    protected VaultAuthConfig(Method method) {
        this.method = method;
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
