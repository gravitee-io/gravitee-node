package io.gravitee.node.api.secrets.runtime.grant;

import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.Optional;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GrantService {
    Optional<Grant> getGrant(String contextId);

    boolean grant(DiscoveryContext context, Spec spec);

    void revoke(DiscoveryContext context);
}
