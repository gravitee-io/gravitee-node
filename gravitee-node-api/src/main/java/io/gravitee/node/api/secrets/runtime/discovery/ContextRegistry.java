package io.gravitee.node.api.secrets.runtime.discovery;

import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ContextRegistry {
    void register(DiscoveryContext context, Definition definition);
    List<DiscoveryContext> findBySpec(Spec spec);
    List<DiscoveryContext> getByDefinition(String envId, Definition definition);
}
