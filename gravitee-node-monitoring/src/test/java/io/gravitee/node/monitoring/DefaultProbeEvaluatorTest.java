package io.gravitee.node.monitoring;

import static io.gravitee.node.monitoring.DefaultProbeEvaluator.SAFEGUARD_DELAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.gravitee.node.api.healthcheck.Result;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DefaultProbeEvaluatorTest {

    protected static final int CACHE_DURATION_MS = 30000;

    @Mock
    private ProbeManager probeManager;

    @Mock
    private Probe probe1;

    @Mock
    private Probe probe2;

    private DefaultProbeEvaluator cut;

    @Test
    void should_complete_and_check_all_probes_when_evaluate() {
        when(probe1.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
        when(probe2.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(probe1, probe2));

        cut = new DefaultProbeEvaluator(0, probeManager);

        final CompletableFuture<Map<Probe, Result>> result = cut.evaluate();

        assertThat(result).isCompletedWithValueMatching(probeResultMap -> {
            assertThat(probeResultMap.get(probe1)).isEqualTo(Result.healthy());
            assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy());
            return true;
        });

        verify(probe1).check();
        verify(probe2).check();
    }

    @Test
    void should_complete_with_probe_unhealthy_when_probe_unhealthy() {
        when(probe1.check()).thenReturn(CompletableFuture.completedFuture(Result.unhealthy("unhealthy")));
        when(probe2.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(probe1, probe2));

        cut = new DefaultProbeEvaluator(0, probeManager);

        final CompletableFuture<Map<Probe, Result>> result = cut.evaluate();

        assertThat(result).isCompletedWithValueMatching(probeResultMap -> {
            assertThat(probeResultMap.get(probe1)).isEqualTo(Result.unhealthy("unhealthy"));
            assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy());
            return true;
        });

        verify(probe1).check();
        verify(probe2).check();
    }

    @Test
    void should_complete_with_probe_unhealthy_when_probe_failed_to_be_evaluated() {
        final Exception error = new Exception("Mock error");

        when(probe1.check()).thenReturn(CompletableFuture.failedFuture(error));
        when(probe2.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(probe1, probe2));

        cut = new DefaultProbeEvaluator(0, probeManager);

        final CompletableFuture<Map<Probe, Result>> result = cut.evaluate();

        assertThat(result).isCompletedWithValueMatching(probeResultMap -> {
            assertThat(probeResultMap.get(probe1)).isEqualTo(Result.unhealthy(error));
            assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy());
            return true;
        });

        verify(probe1).check();
        verify(probe2).check();
    }

    @Test
    void should_use_cached_results_when_safeguard_delay_is_not_elapsed() {
        when(probe1.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
        when(probe2.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(probe1, probe2));

        cut = new DefaultProbeEvaluator(CACHE_DURATION_MS, probeManager);

        // Evaluate first time.
        assertThat(cut.evaluate()).isCompleted();

        // Re-evaluate and check the probe1 cached result has been returned.
        final CompletableFuture<Map<Probe, Result>> result = cut.evaluate();

        assertThat(result).isCompletedWithValueMatching(probeResultMap -> {
            assertThat(probeResultMap.get(probe1)).isEqualTo(Result.healthy());
            assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy());
            return true;
        });

        // Should have been called once.
        verify(probe1).check();
        verify(probe2).check();
    }

    @Test
    @SneakyThrows
    void should_use_cached_results_when_another_evaluation_is_already_in_progress() {
        when(probe1.check()).thenReturn(
            CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Simulating slow probe during for 1 seconds");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                log.info("Simulating slow probe done");
                return Result.healthy();
            })
        );
        when(probe2.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(probe1, probe2));

        cut = new DefaultProbeEvaluator(CACHE_DURATION_MS, probeManager);

        // Evaluate first time.
        CompletableFuture<Map<Probe, Result>> firstEvaluate = cut.evaluate();
        assertThat(firstEvaluate).isNotCompleted();

        // Re-evaluate while the previous evaluation didn't finish yet.
        final CompletableFuture<Map<Probe, Result>> result = cut.evaluate();

        // Cached results are returned.
        assertThat(result).isCompletedWithValueMatching(probeResultMap -> {
            assertThat(probeResultMap.get(probe1)).isNull(); // Probe still evaluating. No cache.
            assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy()); // Probe evaluated, cache is returned.
            return true;
        });

        Map<Probe, Result> probeResultMap = firstEvaluate.get(10, TimeUnit.SECONDS);

        assertThat(probeResultMap.get(probe1)).isEqualTo(Result.healthy());
        assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy());

        // Should have been called once.
        verify(probe1).check();
        verify(probe2).check();
    }

    @Test
    void should_use_cached_result_when_probe_is_cacheable() {
        MockedStatic<Instant> mockedStaticInstant = null;

        try {
            when(probe1.isCacheable()).thenReturn(true);
            when(probe1.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
            when(probe2.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
            when(probeManager.getProbes()).thenReturn(List.of(probe1, probe2));

            cut = new DefaultProbeEvaluator(CACHE_DURATION_MS, probeManager);

            // Evaluate first time.
            assertThat(cut.evaluate()).isCompleted();

            final Instant instant = Instant.now().plus(SAFEGUARD_DELAY + 1000, ChronoUnit.MILLIS);
            mockedStaticInstant = mockStatic(Instant.class);
            mockedStaticInstant.when(Instant::now).thenReturn(instant);

            // Re-evaluate and check the probe1 cached result has been returned.
            final CompletableFuture<Map<Probe, Result>> result = cut.evaluate();

            assertThat(result).isCompletedWithValueMatching(probeResultMap -> {
                assertThat(probeResultMap.get(probe1)).isEqualTo(Result.healthy());
                assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy());
                return true;
            });

            verify(probe1).check();
            verify(probe2, times(2)).check();
        } finally {
            if (mockedStaticInstant != null) {
                mockedStaticInstant.close();
            }
        }
    }

    @Test
    void should_re_evaluate_probe_when_probe_cache_delay_is_elapsed() {
        MockedStatic<Instant> mockedStaticInstant = null;

        try {
            when(probe1.isCacheable()).thenReturn(true);
            when(probe1.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
            when(probe2.check()).thenReturn(CompletableFuture.completedFuture(Result.healthy()));
            when(probeManager.getProbes()).thenReturn(List.of(probe1, probe2));

            cut = new DefaultProbeEvaluator(CACHE_DURATION_MS, probeManager);

            // Evaluate first time.
            assertThat(cut.evaluate()).isCompleted();

            final Instant instant = Instant.now().plus(CACHE_DURATION_MS + 1000, ChronoUnit.MILLIS);
            mockedStaticInstant = mockStatic(Instant.class);
            mockedStaticInstant.when(Instant::now).thenReturn(instant);

            // Re-evaluate and check the probe1 cached result has been returned.
            final CompletableFuture<Map<Probe, Result>> result = cut.evaluate();

            assertThat(result).isCompletedWithValueMatching(probeResultMap -> {
                assertThat(probeResultMap.get(probe1)).isEqualTo(Result.healthy());
                assertThat(probeResultMap.get(probe2)).isEqualTo(Result.healthy());
                return true;
            });

            verify(probe1, times(2)).check();
            verify(probe2, times(2)).check();
        } finally {
            if (mockedStaticInstant != null) {
                mockedStaticInstant.close();
            }
        }
    }

    @Test
    void should_recompute_a_real_time_probe_named_inside_the_safeguard_delay() {
        final AtomicInteger runs = new AtomicInteger();
        final Probe realTime = countingProbe("rt", true, runs);
        when(probeManager.getProbes()).thenReturn(List.of(realTime));
        cut = new DefaultProbeEvaluator(0, probeManager);

        assertThat(cut.evaluate(Set.of("rt"))).isCompleted();
        final Map<Probe, Result> second = cut.evaluate(Set.of("rt")).join();

        assertThat(runs).hasValue(2);
        assertThat(second.get(realTime).getMessage()).isEqualTo("run 2");
    }

    @Test
    void should_recompute_a_real_time_probe_when_a_caller_that_does_not_name_it_ran_just_before() {
        final AtomicInteger runs = new AtomicInteger();
        final Probe realTime = countingProbe("rt", true, runs);
        final Probe other = new TestProbe("other", false, () -> CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(realTime, other));
        cut = new DefaultProbeEvaluator(0, probeManager);

        assertThat(cut.evaluate(Set.of("other"))).isCompleted();
        final Map<Probe, Result> readiness = cut.evaluate(Set.of("other", "rt")).join();

        assertThat(runs).hasValue(1);
        assertThat(readiness.get(realTime).getMessage()).isEqualTo("run 1");
    }

    @Test
    void should_not_recompute_a_real_time_probe_on_an_unfiltered_call() {
        final AtomicInteger runs = new AtomicInteger();
        final Probe realTime = countingProbe("rt", true, runs);
        when(probeManager.getProbes()).thenReturn(List.of(realTime));
        cut = new DefaultProbeEvaluator(0, probeManager);

        assertThat(cut.evaluate()).isCompleted();
        assertThat(cut.evaluate()).isCompleted();

        assertThat(runs).hasValue(1);
    }

    @Test
    void should_not_recompute_a_regular_probe_named_inside_the_safeguard_delay() {
        final AtomicInteger runs = new AtomicInteger();
        final Probe regular = countingProbe("regular", false, runs);
        when(probeManager.getProbes()).thenReturn(List.of(regular));
        cut = new DefaultProbeEvaluator(0, probeManager);

        assertThat(cut.evaluate(Set.of("regular"))).isCompleted();
        assertThat(cut.evaluate(Set.of("regular"))).isCompleted();

        assertThat(runs).hasValue(1);
    }

    @Test
    void should_report_a_probe_that_throws_unhealthy_without_exposing_the_error_message() {
        final Probe fragile = new TestProbe("fragile", true, () -> {
            throw new IllegalStateException("secret-detail");
        });
        when(probeManager.getProbes()).thenReturn(List.of(fragile));
        cut = new DefaultProbeEvaluator(0, probeManager);

        final Result result = cut.evaluate(Set.of("fragile")).join().get(fragile);

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getMessage()).doesNotContain("secret-detail");
    }

    @Test
    void should_not_stay_stuck_when_a_regular_probe_throws_an_error() {
        final AtomicBoolean throwing = new AtomicBoolean(true);
        final Probe fragile = new TestProbe("fragile", false, () -> {
            if (throwing.get()) {
                throw new NoClassDefFoundError("missing");
            }
            return CompletableFuture.completedFuture(Result.healthy());
        });
        when(probeManager.getProbes()).thenReturn(List.of(fragile));
        cut = new DefaultProbeEvaluator(0, probeManager);

        assertThat(cut.evaluate().join().get(fragile).isHealthy()).isFalse();

        throwing.set(false);
        final Instant later = Instant.now().plus(SAFEGUARD_DELAY + 1000, ChronoUnit.MILLIS);
        try (MockedStatic<Instant> mocked = mockStatic(Instant.class)) {
            mocked.when(Instant::now).thenReturn(later);
            assertThat(cut.evaluate().join().get(fragile).isHealthy()).isTrue();
        }
    }

    @Test
    void should_report_unhealthy_when_a_probe_completes_with_null() {
        final Probe nullProbe = new TestProbe("null", true, () -> CompletableFuture.completedFuture(null));
        when(probeManager.getProbes()).thenReturn(List.of(nullProbe));
        cut = new DefaultProbeEvaluator(0, probeManager);

        final CompletableFuture<Map<Probe, Result>> result = cut.evaluate(Set.of("null"));

        assertThat(result).isCompleted();
        assertThat(result.join().get(nullProbe).isHealthy()).isFalse();
    }

    @Test
    void should_recompute_a_named_real_time_probe_while_another_evaluation_is_in_progress() {
        final CompletableFuture<Result> slow = new CompletableFuture<>();
        final Probe liveness = new TestProbe("liveness", false, () -> slow);
        final AtomicInteger runs = new AtomicInteger();
        final Probe capacity = countingProbe("capacity", true, runs);
        when(probeManager.getProbes()).thenReturn(List.of(liveness, capacity));
        cut = new DefaultProbeEvaluator(0, probeManager);

        final CompletableFuture<Map<Probe, Result>> first = cut.evaluate(Set.of("liveness"));
        assertThat(first).isNotCompleted();

        final CompletableFuture<Map<Probe, Result>> second = cut.evaluate(Set.of("capacity"));

        assertThat(second).isCompleted();
        assertThat(second.join().get(capacity)).isNotNull();
        assertThat(runs).hasValue(1);
        slow.complete(Result.healthy());
    }

    @Test
    void should_not_recompute_a_real_time_probe_that_the_call_does_not_name() {
        final AtomicInteger runs = new AtomicInteger();
        final Probe realTime = countingProbe("rt", true, runs);
        final Probe other = new TestProbe("other", false, () -> CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(realTime, other));
        cut = new DefaultProbeEvaluator(0, probeManager);

        assertThat(cut.evaluate(Set.of("rt", "other"))).isCompleted();
        assertThat(cut.evaluate(Set.of("other"))).isCompleted();

        assertThat(runs).hasValue(1);
    }

    @Test
    void should_evaluate_a_named_probe_that_has_no_result_yet_inside_the_safeguard_delay() {
        final Probe a = new TestProbe("a", false, () -> CompletableFuture.completedFuture(Result.healthy()));
        final Probe b = new TestProbe("b", false, () -> CompletableFuture.completedFuture(Result.healthy()));
        when(probeManager.getProbes()).thenReturn(List.of(a, b));
        cut = new DefaultProbeEvaluator(0, probeManager);

        assertThat(cut.evaluate(Set.of("a"))).isCompleted();
        final Map<Probe, Result> second = cut.evaluate(Set.of("b")).join();

        assertThat(second.get(b)).isNotNull();
    }

    @Test
    void should_ignore_the_cache_of_a_probe_that_is_both_real_time_and_cacheable() {
        final AtomicInteger runs = new AtomicInteger();
        final Probe both = new CacheableTestProbe("both", () -> countingResult(runs));
        when(probeManager.getProbes()).thenReturn(List.of(both));
        cut = new DefaultProbeEvaluator(CACHE_DURATION_MS, probeManager);

        assertThat(cut.evaluate()).isCompleted();
        final Instant later = Instant.now().plus(SAFEGUARD_DELAY + 1000, ChronoUnit.MILLIS);
        try (MockedStatic<Instant> mocked = mockStatic(Instant.class)) {
            mocked.when(Instant::now).thenReturn(later);
            assertThat(cut.evaluate()).isCompleted();
        }

        assertThat(runs).hasValue(2);
    }

    private static Probe countingProbe(String id, boolean realTime, AtomicInteger runs) {
        return new TestProbe(id, realTime, () -> countingResult(runs));
    }

    private static CompletableFuture<Result> countingResult(AtomicInteger runs) {
        return CompletableFuture.completedFuture(Result.healthy("run " + runs.incrementAndGet()));
    }

    private record TestProbe(String id, boolean isRealTime, Supplier<CompletableFuture<Result>> supplier) implements Probe {
        @Override
        public CompletableFuture<Result> check() {
            return supplier.get();
        }
    }

    private record CacheableTestProbe(String id, Supplier<CompletableFuture<Result>> supplier) implements Probe {
        @Override
        public boolean isRealTime() {
            return true;
        }

        @Override
        public boolean isCacheable() {
            return true;
        }

        @Override
        public CompletableFuture<Result> check() {
            return supplier.get();
        }
    }
}
