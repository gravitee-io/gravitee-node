package io.gravitee.node.secrets.plugin.mock;

import io.gravitee.secrets.api.core.SecretURL;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record MockSecretLocation(String secret) {
    static MockSecretLocation fromUrl(SecretURL url) {
        return new MockSecretLocation(url.path());
    }
}
