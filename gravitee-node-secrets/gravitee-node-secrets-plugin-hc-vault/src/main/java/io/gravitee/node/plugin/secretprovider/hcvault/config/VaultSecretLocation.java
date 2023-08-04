package io.gravitee.node.plugin.secretprovider.hcvault.config;

import static io.gravitee.node.plugin.secretprovider.hcvault.HCVaultSecretProvider.PLUGIN_ID;
import static io.gravitee.node.secrets.api.SecretProvider.PLUGIN_URL_SCHEME;

import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.api.model.SecretLocation;
import io.gravitee.node.secrets.api.model.SecretURL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record VaultSecretLocation(String secretPath, String key) {
    private static final String LOCATION_SECRET = "secretPath";
    private static final String LOCATION_KEY = "key";

    public Map<String, Object> asMap() {
        return Map.of(LOCATION_SECRET, secretPath, LOCATION_KEY, key);
    }

    public static VaultSecretLocation fromLocation(SecretLocation location) {
        return new VaultSecretLocation(Objects.requireNonNull(location.get(LOCATION_SECRET)), location.get(LOCATION_KEY));
    }

    public static VaultSecretLocation fromURL(SecretURL url) {
        List<String> elements = url.pathAsList();
        if (elements.size() >= 2) {
            int last = elements.size() - 1;

            return new VaultSecretLocation(
                String.join(String.valueOf(SecretURL.URL_SEPARATOR), elements.subList(0, last)),
                elements.get(last)
            );
        }
        throw new SecretManagerConfigurationException(
            "URL is not valid for Kubernetes Secret Provider plugin. Should be %s%s/<mount>/<secret path>/<data field> but was: '%s'".formatted(
                    PLUGIN_URL_SCHEME,
                    PLUGIN_ID,
                    url
                )
        );
    }
}
