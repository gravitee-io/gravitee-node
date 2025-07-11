package io.gravitee.node.monitoring.healthcheck.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.spring.HealthConfiguration;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class CPUProbeTest {

    @Test
    @SneakyThrows
    void should_be_healthy_when_cpu_threshold_is_not_reached() {
        final CPUProbe cpuProbe = new CPUProbe(new HealthConfiguration(true, 0, TimeUnit.MILLISECONDS, Integer.MAX_VALUE, 80, 20));

        final Result result = cpuProbe.check().get();

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    @SneakyThrows
    void should_be_unhealthy_when_cpu_threshold_is_reached() {
        final CPUProbe cpuProbe = new CPUProbe(new HealthConfiguration(true, 0, TimeUnit.MILLISECONDS, Integer.MIN_VALUE, 80, 20));

        final Result result = cpuProbe.check().get();

        assertThat(result.isHealthy()).isFalse();
    }
}
