package io.gravitee.node.api.secrets.runtime.spec;

import io.gravitee.node.api.secrets.runtime.discovery.Ref;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SpecLifecycleService {
    boolean shouldDeployOnTheFly(Ref ref);
    Spec deployOnTheFly(String envId, Ref ref);

    void deploy(Spec spec);

    void undeploy(Spec spec);
}
