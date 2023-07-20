package io.gravitee.node.plugin.secretprovider.kubernetes.config;

import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.model.SecretMount;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class KubernetesConfiguration implements SecretManagerConfiguration {

    public static final String LOCATION_NAMESPACE = "namespace";
    public static final String LOCATION_SECRET = "secret";
    public static final String LOCATION_FIELD = "field";

    private int priority;
    private boolean enabled;
    private String kubeConfigFile;

    // called by introspections
    public KubernetesConfiguration(Map<String, Object> properties) {
        Objects.requireNonNull(properties);
        enabled = (boolean) properties.getOrDefault(Fields.enabled, false);
        priority = (int) properties.getOrDefault(Fields.priority, 0);
        kubeConfigFile = (String) properties.getOrDefault(Fields.kubeConfigFile, "");
    }

    public static String getNamespace(SecretMount secretMount) {
        return (String) secretMount.location().get(LOCATION_NAMESPACE);
    }

    public static String getSecret(SecretMount secretMount) {
        return (String) secretMount.location().get(LOCATION_SECRET);
    }

    public static String getField(SecretMount secretMount) {
        return (String) secretMount.location().get(LOCATION_FIELD);
    }
}
