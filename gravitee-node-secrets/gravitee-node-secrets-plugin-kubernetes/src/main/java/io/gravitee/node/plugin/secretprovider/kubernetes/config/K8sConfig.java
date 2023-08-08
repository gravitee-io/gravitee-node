package io.gravitee.node.plugin.secretprovider.kubernetes.config;

import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class K8sConfig implements SecretManagerConfiguration {

    private boolean enabled;
    private String kubeConfigFile;
    private int timeoutMs;
    private String namespace;

    // called by introspection
    public K8sConfig(Map<String, Object> properties) {
        Objects.requireNonNull(properties);
        enabled = (boolean) properties.getOrDefault(Fields.enabled, false);
        if (!isEnabled()) {
            return;
        }
        kubeConfigFile = (String) properties.getOrDefault(Fields.kubeConfigFile, "");
        timeoutMs = (int) properties.getOrDefault(Fields.timeoutMs, 3000);
        namespace = (String) properties.getOrDefault(Fields.namespace, "default");
    }

    public boolean isClusterBased() {
        return kubeConfigFile.isBlank();
    }
}
