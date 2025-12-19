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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

        assertThat(result)
            .isCompletedWithValueMatching(probeResultMap -> {
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

        assertThat(result)
            .isCompletedWithValueMatching(probeResultMap -> {
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

        assertThat(result)
            .isCompletedWithValueMatching(probeResultMap -> {
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

        assertThat(result)
            .isCompletedWithValueMatching(probeResultMap -> {
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
        when(probe1.check())
            .thenReturn(
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
        assertThat(result)
            .isCompletedWithValueMatching(probeResultMap -> {
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

            assertThat(result)
                .isCompletedWithValueMatching(probeResultMap -> {
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

            assertThat(result)
                .isCompletedWithValueMatching(probeResultMap -> {
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
}
