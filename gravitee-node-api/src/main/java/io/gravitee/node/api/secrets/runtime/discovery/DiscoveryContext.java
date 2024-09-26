package io.gravitee.node.api.secrets.runtime.discovery;

import java.util.UUID;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record DiscoveryContext(UUID id, String envId, Ref ref, DiscoveryLocation location) {}
