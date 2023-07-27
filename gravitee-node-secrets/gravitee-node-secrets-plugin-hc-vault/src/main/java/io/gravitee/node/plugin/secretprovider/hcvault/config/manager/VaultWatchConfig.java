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
public class VaultWatchConfig {

    private boolean enabled;
    private int pollIntervalSec;

    public VaultWatchConfig(Map<String, Object> properties) {
        enabled = (boolean) properties.getOrDefault(Fields.enabled, false);
        pollIntervalSec = (int) properties.getOrDefault(Fields.pollIntervalSec, 30);
    }
}
