package io.gravitee.node.monitoring;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.ProbeEvaluator;
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.gravitee.node.api.healthcheck.Result;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DefaultProbeEvaluator implements ProbeEvaluator {

    protected static final long SAFEGUARD_DELAY = 5000L;

    private final long cacheDurationMs;
    private final ProbeManager probeManager;
    private final Map<Probe, Result> lastProbeResults = new ConcurrentHashMap<>();
    private Long lastEvaluation;

    private final AtomicBoolean evaluating = new AtomicBoolean(false);

    @Override
    public CompletableFuture<Map<Probe, Result>> evaluate() {
        return evaluate(Set.of());
    }

    @Override
    public CompletableFuture<Map<Probe, Result>> evaluate(final Set<String> probeIds) {
        final long now = Instant.now().toEpochMilli();
        final long elapsedTime = lastEvaluation != null ? now - lastEvaluation : Long.MAX_VALUE;

        if (elapsedTime < SAFEGUARD_DELAY) {
            // Avoid too much pressure evaluating probes.
            return CompletableFuture.completedFuture(lastProbeResults);
        }

        // Make sure the previous evaluation is finished.
        if (evaluating.compareAndSet(false, true)) {
            final List<CompletableFuture<Void>> collect =
                this.probeManager.getProbes()
                    .stream()
                    .filter(probe -> probeIds == null || probeIds.isEmpty() || probeIds.contains(probe.id()))
                    .map(probe -> {
                        final Result lastProbeResult = lastProbeResults.get(probe);
                        if (lastProbeResult != null && probe.isCacheable() && (now - lastProbeResult.timestamp()) < cacheDurationMs) {
                            // The probe has been evaluated once and the elapsed time is below the cache limit. Don't re-evaluate.
                            return CompletableFuture.<Void>completedFuture(null);
                        }

                        // Evaluate the probe and update the probe map.
                        return probe
                            .check()
                            .exceptionally(Result::unhealthy)
                            .thenAccept(result -> lastProbeResults.compute(probe, (probe1, result1) -> result))
                            .toCompletableFuture();
                    })
                    .toList();

            // Ensure all the probes have been resolved and return all the results.
            return CompletableFuture
                .allOf(collect.toArray(new CompletableFuture[0]))
                .thenApply(unused -> lastProbeResults)
                .whenComplete((probeResultMap, throwable) -> {
                    evaluating.set(false);

                    if (throwable == null) {
                        lastEvaluation = now;
                    }
                });
        } else {
            return CompletableFuture.completedFuture(lastProbeResults);
        }
    }

    public Map<Probe, Result> getCachedResults() {
        return lastProbeResults;
    }
}
