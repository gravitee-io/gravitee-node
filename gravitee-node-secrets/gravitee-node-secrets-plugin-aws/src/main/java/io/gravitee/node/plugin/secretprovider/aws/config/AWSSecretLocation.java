package io.gravitee.node.plugin.secretprovider.aws.config;

import io.gravitee.node.api.secrets.model.SecretLocation;
import io.gravitee.node.api.secrets.model.SecretURL;
import java.util.Map;
import java.util.Objects;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public record AWSSecretLocation(String secretName, String key) {
    private static final String LOCATION_NAME = "name";
    private static final String LOCATION_KEY = "key";

    public Map<String, Object> asMap() {
        return Map.of(LOCATION_NAME, secretName, LOCATION_KEY, key);
    }

    public static AWSSecretLocation fromLocation(SecretLocation location) {
        return new AWSSecretLocation(Objects.requireNonNull(location.get(LOCATION_NAME)), location.get(LOCATION_KEY));
    }

    public static AWSSecretLocation fromURL(SecretURL url) {
        return new AWSSecretLocation(url.path(), url.key());
    }
}
