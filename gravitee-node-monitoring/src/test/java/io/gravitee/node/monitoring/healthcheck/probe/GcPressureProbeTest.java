package io.gravitee.node.monitoring.healthcheck.probe;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.spring.HealthConfiguration;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
class GcPressureProbeTest {

    @Test
    @SneakyThrows
    void should_be_healthy_when_gc_pressure_threshold_is_not_reached() {
        final GcPressureProbe gcPressureProbe = new GcPressureProbe(
            new HealthConfiguration(true, 0, TimeUnit.MILLISECONDS, 80, 80, Integer.MAX_VALUE)
        );

        final Result result = gcPressureProbe.check().get();

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    @SneakyThrows
    void should_be_unhealthy_when_gc_pressure_threshold_is_reached() {
        final GcPressureProbe gcPressureProbe = new GcPressureProbe(
            new HealthConfiguration(true, 0, TimeUnit.MILLISECONDS, 80, 80, Integer.MIN_VALUE)
        );

        final Result result = gcPressureProbe.check().get();

        assertThat(result.isHealthy()).isFalse();
    }
}
