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
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
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
            // Avoid too much pressure evaluating probes, except for the named probes that are real time or have no result yet.
            return evaluateNamedProbes(probeIds);
        }

        // Make sure the previous evaluation is finished.
        if (evaluating.compareAndSet(false, true)) {
            final List<CompletableFuture<Void>> collect = this.probeManager.getProbes()
                .stream()
                .filter(probe -> probeIds == null || probeIds.isEmpty() || probeIds.contains(probe.id()))
                .map(probe -> {
                    final Result lastProbeResult = lastProbeResults.get(probe);
                    if (
                        lastProbeResult != null &&
                        probe.isCacheable() &&
                        !probe.isRealTime() &&
                        (now - lastProbeResult.timestamp()) < cacheDurationMs
                    ) {
                        // The probe has been evaluated once and the elapsed time is below the cache limit. Don't re-evaluate.
                        return CompletableFuture.<Void>completedFuture(null);
                    }

                    // Evaluate the probe and update the probe map.
                    return evaluateProbe(probe);
                })
                .toList();

            // Ensure all the probes have been resolved and return all the results.
            return CompletableFuture.allOf(collect.toArray(new CompletableFuture[0]))
                .thenApply(unused -> lastProbeResults)
                .whenComplete((probeResultMap, throwable) -> {
                    evaluating.set(false);

                    if (throwable == null) {
                        lastEvaluation = now;
                    }
                });
        } else {
            return evaluateNamedProbes(probeIds);
        }
    }

    private CompletableFuture<Map<Probe, Result>> evaluateNamedProbes(final Set<String> probeIds) {
        if (probeIds == null || probeIds.isEmpty()) {
            return CompletableFuture.completedFuture(lastProbeResults);
        }
        final List<CompletableFuture<Void>> toEvaluate = this.probeManager.getProbes()
            .stream()
            .filter(probe -> probeIds.contains(probe.id()) && (probe.isRealTime() || !lastProbeResults.containsKey(probe)))
            .map(this::evaluateProbe)
            .toList();
        return CompletableFuture.allOf(toEvaluate.toArray(new CompletableFuture[0])).thenApply(unused -> lastProbeResults);
    }

    private CompletableFuture<Void> evaluateProbe(final Probe probe) {
        CompletableFuture<Result> check;
        try {
            check = probe.check().toCompletableFuture();
        } catch (Exception | LinkageError t) {
            // A probe that throws (including class loading errors) instead of returning a failed stage must not leave the evaluation stuck.
            log.warn("Probe [{}] failed with an unexpected error", probe.id(), t);
            check = CompletableFuture.failedFuture(new IllegalStateException("Probe check failed"));
        }
        return check
            .exceptionally(Result::unhealthy)
            .thenAccept(result -> lastProbeResults.put(probe, result == null ? Result.unhealthy("No result") : result));
    }

    public Map<Probe, Result> getCachedResults() {
        return lastProbeResults;
    }
}
