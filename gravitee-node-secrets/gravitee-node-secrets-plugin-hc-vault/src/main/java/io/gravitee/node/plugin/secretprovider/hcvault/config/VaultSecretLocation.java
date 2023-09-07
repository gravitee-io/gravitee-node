package io.gravitee.node.plugin.secretprovider.hcvault.config;

import static io.gravitee.node.secrets.api.SecretProvider.PLUGIN_URL_SCHEME;

import io.gravitee.node.api.secrets.errors.SecretManagerConfigurationException;
import io.gravitee.node.api.secrets.model.SecretLocation;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.plugin.secretprovider.hcvault.config.manager.VaultConfig;
import java.util.Map;
import java.util.Objects;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record VaultSecretLocation(String namespace, String secretPath, String key) {
    private static final String LOCATION_SECRET_PATH = "secretPath";
    private static final String LOCATION_KEY = "key";
    private static final String LOCATION_NAMESPACE = "namespace";

    public Map<String, Object> asMap() {
        return Map.of(LOCATION_NAMESPACE, namespace, LOCATION_SECRET_PATH, secretPath, LOCATION_KEY, key);
    }

    public static VaultSecretLocation fromLocation(SecretLocation location) {
        return new VaultSecretLocation(
            Objects.requireNonNull(location.get(LOCATION_NAMESPACE)),
            Objects.requireNonNull(location.get(LOCATION_SECRET_PATH)),
            location.get(LOCATION_KEY)
        );
    }

    public static VaultSecretLocation fromURL(SecretURL url, VaultConfig vaultConfig) {
        return new VaultSecretLocation(
            url.query().get(SecretURL.WellKnownQueryParam.NAMESPACE).stream().findFirst().orElse(vaultConfig.getNamespace()),
            url.path(),
            url.key()
        );
    }
}
