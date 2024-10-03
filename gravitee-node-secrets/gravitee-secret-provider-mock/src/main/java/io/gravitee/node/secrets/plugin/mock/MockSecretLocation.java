package io.gravitee.node.secrets.plugin.mock;

import io.gravitee.node.api.secrets.model.SecretLocation;
import io.gravitee.node.api.secrets.model.SecretURL;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record MockSecretLocation(String secret) {
    static SecretLocation fromUrl(SecretURL url) {
        return new MockSecretLocation(url.path()).toLocation();
    }

    static MockSecretLocation fromLocation(SecretLocation location) {
        return new MockSecretLocation(location.get("secret"));
    }

    SecretLocation toLocation() {
        return new SecretLocation(Map.of("secret", secret));
    }
}