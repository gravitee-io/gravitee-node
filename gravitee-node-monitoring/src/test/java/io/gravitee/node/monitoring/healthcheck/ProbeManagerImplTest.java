package io.gravitee.node.monitoring.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.monitoring.healthcheck.probe.CPUProbe;
import io.gravitee.node.monitoring.healthcheck.probe.MemoryProbe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ProbeManagerImplTest {

    @Mock
    private Probe probe;

    private ProbeManagerImpl cut;

    @BeforeEach
    void beforeEach() {
        cut = new ProbeManagerImpl();
        cut.setApplicationContext(new AnnotationConfigApplicationContext());
        lenient().when(probe.id()).thenReturn("probeId");
    }

    @Test
    void should_discover_and_register_cpu_and_memory_probes() {
        final List<Probe> probes = cut.getProbes();
        assertThat(probes).hasSize(2).hasOnlyElementsOfTypes(CPUProbe.class, MemoryProbe.class);
    }

    @Test
    void should_register_probe() {
        cut.register(probe);
        assertThat(cut.getProbes()).contains(probe);
    }

    @Test
    void should_not_register_cpu_probe_twice() {
        CPUProbe cpuProbe = new CPUProbe();
        cut.register(cpuProbe);
        assertThat(cut.getProbes()).hasSize(2).contains(cpuProbe);
    }
}
