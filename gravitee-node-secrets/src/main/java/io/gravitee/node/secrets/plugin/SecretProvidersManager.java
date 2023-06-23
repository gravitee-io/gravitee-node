package io.gravitee.node.secrets.plugin;

import io.gravitee.node.secrets.SecretProviderService;
import java.util.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretProvidersManager /* implements ConfigurablePluginManager */{

    public Optional<SecretProviderService> createSecretProviderInstance(String scheme, String configuration) {
        return Optional.empty(); // TODO re-implement
    }
}
