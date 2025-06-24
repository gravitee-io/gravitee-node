package io.gravitee.node.monitoring.healthcheck;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.monitoring.DefaultProbeEvaluator;
import io.gravitee.node.monitoring.spring.HealthConfiguration;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class NodeHealthCheckServiceTest {

    protected static final String NODE_ID = "NODE_ID";

    @Mock
    private ManagementEndpointManager managementEndpointManager;

    @Mock
    private DefaultProbeEvaluator probeRegistry;

    @Mock
    private NodeHealthCheckManagementEndpoint healthCheckEndpoint;

    @Mock
    private AlertEventProducer alertEventProducer;

    @Mock
    private Node node;

    @Test
    @SneakyThrows
    void should_start_the_service_when_enabled() {
        final NodeHealthCheckService cut = new NodeHealthCheckService(
            managementEndpointManager,
            probeRegistry,
            healthCheckEndpoint,
            alertEventProducer,
            node,
            Vertx.vertx(),
            new HealthConfiguration(true, 1, MILLISECONDS, 0, 0, 0)
        );

        try (MockedStatic<BackendRegistries> backendRegistries = Mockito.mockStatic(BackendRegistries.class)) {
            backendRegistries.when(BackendRegistries::getDefaultNow).thenReturn(mock(PrometheusMeterRegistry.class));

            cut.doStart();

            verify(managementEndpointManager).register(healthCheckEndpoint);
            verify(probeRegistry, atLeastOnce()).evaluate();
        }
    }

    @Test
    @SneakyThrows
    void should_not_start_the_service_when_disabled() {
        final NodeHealthCheckService cut = new NodeHealthCheckService(
            managementEndpointManager,
            probeRegistry,
            healthCheckEndpoint,
            alertEventProducer,
            node,
            Vertx.vertx(),
            new HealthConfiguration(false, 1, MILLISECONDS, 0, 0, 0)
        );

        cut.doStart();

        verify(managementEndpointManager, never()).register(healthCheckEndpoint);
        verify(probeRegistry, never()).evaluate();
    }
}
