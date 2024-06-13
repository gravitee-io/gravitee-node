package io.gravitee.node.api.healthcheck;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This registry that holds all the registered probes (see {@link ProbeManager}) and allows evaluating them in a single call.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ProbeEvaluator {
    /**
     * Evaluate all the registered probes.
     * In case a {@link Probe} is cacheable, the probe will be checked again only if the time elapsed since the last evaluation is above a particular threshold.
     *
     * @return a {@link CompletableFuture} with the map of all evaluated probes.
     */
    CompletableFuture<Map<Probe, Result>> evaluate();
}
