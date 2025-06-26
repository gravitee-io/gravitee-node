package io.gravitee.node.monitoring.healthcheck.probe;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.spring.HealthConfiguration;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class MemoryProbeTest {

    @Test
    @SneakyThrows
    void should_be_healthy_when_memory_threshold_is_not_reached() {
        final MemoryProbe memoryProbe = new MemoryProbe(new HealthConfiguration(true, 0, TimeUnit.MILLISECONDS, 80, Integer.MAX_VALUE, 20));

        final Result result = memoryProbe.check().get();

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    @SneakyThrows
    void should_be_unhealthy_when_memory_threshold_is_reached() {
        final MemoryProbe memoryProbe = new MemoryProbe(new HealthConfiguration(true, 0, TimeUnit.MILLISECONDS, 80, Integer.MIN_VALUE, 20));

        final Result result = memoryProbe.check().get();

        assertThat(result.isHealthy()).isFalse();
    }
}
