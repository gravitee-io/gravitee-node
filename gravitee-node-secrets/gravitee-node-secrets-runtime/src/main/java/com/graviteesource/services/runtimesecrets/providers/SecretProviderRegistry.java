package com.graviteesource.services.runtimesecrets.providers;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.graviteesource.services.runtimesecrets.errors.SecretProviderNotFoundException;
import io.gravitee.node.api.secrets.SecretProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretProviderRegistry {

    Multimap<String, SecretProviderEntry> perEnv = MultimapBuilder.hashKeys().arrayListValues().build();
    Map<String, SecretProvider> allEnvs = new HashMap<>();

    public void register(String id, SecretProvider provider, String envId) {
        if (envId == null || envId.isEmpty()) {
            allEnvs.put(id, provider);
        } else {
            perEnv.put(envId, new SecretProviderEntry(id, provider));
        }
    }

    /**
     *
     * @param envId environment ID
     * @param id is of the provider
     * @return a secret provider
     * @throws SecretProviderNotFoundException if the provider is not found
     */
    public SecretProvider get(String envId, String id) {
        return perEnv
            .get(envId)
            .stream()
            .filter(entry -> entry.id().equals(id))
            .map(SecretProviderEntry::provider)
            .findFirst()
            .or(() -> Optional.ofNullable(allEnvs.get(id)))
            .orElseThrow(() ->
                new SecretProviderNotFoundException("Cannot find secret provider with id [%s] for environmentID [%s]".formatted(id, envId))
            );
    }

    public record SecretProviderEntry(String id, SecretProvider provider) {}
}
