package io.gravitee.node.api.secrets.runtime.providers;

import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProviderDeployer {
    void deploy(String id, String pluginId, Map<String, Object> config, String envId);
}
