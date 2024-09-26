package io.gravitee.node.api.secrets.runtime.grant;

import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.spec.Spec;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GrantService {
    boolean isGranted(String token);

    boolean authorize(DiscoveryContext context, Spec spec);

    void grant(DiscoveryContext context);

    void revoke(DiscoveryContext context);
}
