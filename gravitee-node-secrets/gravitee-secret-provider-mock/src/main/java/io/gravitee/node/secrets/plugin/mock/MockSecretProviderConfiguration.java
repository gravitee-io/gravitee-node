package io.gravitee.node.secrets.plugin.mock;

import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.util.ConfigHelper;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@FieldNameConstants
public class MockSecretProviderConfiguration implements SecretManagerConfiguration {

    private final boolean enabled;

    @Getter
    private final Map<String, Object> secrets;

    public MockSecretProviderConfiguration(Map<String, Object> config) {
        this.enabled = ConfigHelper.getProperty(config, Fields.enabled, Boolean.class, false);
        this.secrets = ConfigHelper.removePrefix(config, "secrets");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
