package io.gravitee.node.api.secrets.runtime.providers;

import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProviderDeployer {
    default void init() {}

    void deploy(String pluginId, Map<String, Object> config, String providerId, String envId);
}
