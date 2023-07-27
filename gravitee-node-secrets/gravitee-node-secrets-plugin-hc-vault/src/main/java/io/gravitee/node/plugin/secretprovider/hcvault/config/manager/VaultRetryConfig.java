package io.gravitee.node.plugin.secretprovider.hcvault.config.manager;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants(level = AccessLevel.PACKAGE)
public class VaultRetryConfig {

    private int attempts;
    private int intervalMs;

    public VaultRetryConfig(Map<String, Object> properties) {
        this.attempts = (int) properties.getOrDefault(Fields.attempts, 0);
        this.intervalMs = (int) properties.getOrDefault(Fields.intervalMs, 1000);
    }
}
