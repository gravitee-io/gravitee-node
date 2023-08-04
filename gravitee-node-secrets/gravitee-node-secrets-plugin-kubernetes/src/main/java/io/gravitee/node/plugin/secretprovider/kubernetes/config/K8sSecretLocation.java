package io.gravitee.node.plugin.secretprovider.kubernetes.config;

import static io.gravitee.node.plugin.secretprovider.kubernetes.KubernetesSecretProvider.PLUGIN_ID;
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
public record K8sSecretLocation(String namespace, String secret, String key) {
    private static final String LOCATION_NAMESPACE = "namespace";
    private static final String LOCATION_SECRET = "secret";
    private static final String LOCATION_KEY = "key";

    public Map<String, Object> asMap() {
        if (key == null) {
            return Map.of(LOCATION_NAMESPACE, namespace, LOCATION_SECRET, secret);
        }
        return Map.of(LOCATION_NAMESPACE, namespace, LOCATION_SECRET, secret, LOCATION_KEY, key);
    }

    public static K8sSecretLocation fromLocation(SecretLocation location) {
        return new K8sSecretLocation(
            Objects.requireNonNull(location.get(LOCATION_NAMESPACE)),
            Objects.requireNonNull(location.get(LOCATION_SECRET)),
            location.get(LOCATION_KEY)
        );
    }

    public static K8sSecretLocation fromURL(SecretURL url, K8sConfig k8sConfig) {
        String namespace = url.query().get(SecretURL.WellKnownQueryParam.NAMESPACE).stream().findFirst().orElse(k8sConfig.getNamespace());

        List<String> elements = url.pathAsList();
        if (elements.size() == 1) {
            return new K8sSecretLocation(namespace, elements.get(0), null);
        }
        if (elements.size() == 2) {
            return new K8sSecretLocation(namespace, elements.get(0), elements.get(1));
        }
        throw new SecretManagerConfigurationException(
            "URL is not valid for Kubernetes Secret Provider plugin. Should be %s%s/<secret name>[/<field in secret>][?namespace=<name> but was: '%s'".formatted(
                    PLUGIN_URL_SCHEME,
                    PLUGIN_ID,
                    url
                )
        );
    }
}
