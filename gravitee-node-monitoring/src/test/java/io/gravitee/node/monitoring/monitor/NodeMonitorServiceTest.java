package io.gravitee.node.monitoring.monitor;

import static io.gravitee.node.monitoring.MonitoringConstants.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.alert.api.event.Event;
import io.gravitee.node.api.Node;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.monitoring.spring.MonitoringConfiguration;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class NodeMonitorServiceTest {

    protected static final String NODE_ID = "NODE_ID";

    @Mock
    private NodeMonitorManagementEndpoint nodeMonitorManagementEndpoint;

    @Mock
    private ManagementEndpointManager managementEndpointManager;

    @Mock
    private AlertEventProducer alertEventProducer;

    @Mock
    private Node node;

    @Test
    @SneakyThrows
    void should_start_the_service_when_enabled() {
        final NodeMonitorService cut = new NodeMonitorService(
            nodeMonitorManagementEndpoint,
            managementEndpointManager,
            alertEventProducer,
            node,
            Vertx.vertx(),
            new MonitoringConfiguration(true, 1, MILLISECONDS)
        );

        final Set<String> orgIds = Set.of("ORG_ID");
        final Set<String> envIds = Set.of("ENV_ID");

        when(node.id()).thenReturn(NODE_ID);
        when(node.application()).thenReturn("APPLICATION");
        when(node.hostname()).thenReturn("HOSTNAME");
        when(node.metadata()).thenReturn(Map.of(Node.META_ORGANIZATIONS, orgIds, Node.META_ENVIRONMENTS, envIds));

        cut.doStart();

        verify(alertEventProducer)
            .send(
                argThat(event -> {
                    assertThat(event.type()).isEqualTo(NODE_LIFECYCLE);
                    assertThat(event.properties().get(PROPERTY_NODE_EVENT)).isEqualTo(NODE_EVENT_START);
                    assertThat(event.properties().get(PROPERTY_NODE_ID)).isEqualTo(NODE_ID);
                    assertThat(event.properties().get(PROPERTY_NODE_HOSTNAME)).isEqualTo("HOSTNAME");
                    assertThat(event.properties().get(PROPERTY_NODE_APPLICATION)).isEqualTo("APPLICATION");
                    assertThat(event.properties().get(Event.PROPERTY_ORGANIZATION)).isEqualTo("ORG_ID");
                    assertThat(event.properties().get(Event.PROPERTY_ENVIRONMENT)).isEqualTo("ENV_ID");

                    return true;
                })
            );
    }

    @Test
    @SneakyThrows
    void should_not_start_the_service_when_disabled() {
        final NodeMonitorService cut = new NodeMonitorService(
            nodeMonitorManagementEndpoint,
            managementEndpointManager,
            alertEventProducer,
            node,
            Vertx.vertx(),
            new MonitoringConfiguration(false, 1, MILLISECONDS)
        );

        cut.doStart();

        verify(alertEventProducer, never()).send(any());
    }

    @Test
    @SneakyThrows
    void should_pre_stop_the_service_when_enabled() {
        final NodeMonitorService cut = new NodeMonitorService(
            nodeMonitorManagementEndpoint,
            managementEndpointManager,
            alertEventProducer,
            node,
            Vertx.vertx(),
            new MonitoringConfiguration(true, 1, MILLISECONDS)
        );

        final Set<String> orgIds = Set.of("ORG_ID");
        final Set<String> envIds = Set.of("ENV_ID");

        when(node.id()).thenReturn(NODE_ID);
        when(node.application()).thenReturn("APPLICATION");
        when(node.hostname()).thenReturn("HOSTNAME");
        when(node.metadata()).thenReturn(Map.of(Node.META_ORGANIZATIONS, orgIds, Node.META_ENVIRONMENTS, envIds));

        cut.preStop();

        verify(alertEventProducer)
            .send(
                argThat(event -> {
                    assertThat(event.type()).isEqualTo(NODE_LIFECYCLE);
                    assertThat(event.properties().get(PROPERTY_NODE_EVENT)).isEqualTo(NODE_EVENT_STOP);
                    assertThat(event.properties().get(PROPERTY_NODE_ID)).isEqualTo(NODE_ID);
                    assertThat(event.properties().get(PROPERTY_NODE_HOSTNAME)).isEqualTo("HOSTNAME");
                    assertThat(event.properties().get(PROPERTY_NODE_APPLICATION)).isEqualTo("APPLICATION");
                    assertThat(event.properties().get(Event.PROPERTY_ORGANIZATION)).isEqualTo("ORG_ID");
                    assertThat(event.properties().get(Event.PROPERTY_ENVIRONMENT)).isEqualTo("ENV_ID");

                    return true;
                })
            );
    }

    @Test
    @SneakyThrows
    void should_not_pre_stop_the_service_when_disabled() {
        final NodeMonitorService cut = new NodeMonitorService(
            nodeMonitorManagementEndpoint,
            managementEndpointManager,
            alertEventProducer,
            node,
            Vertx.vertx(),
            new MonitoringConfiguration(false, 1, MILLISECONDS)
        );

        cut.preStop();

        verify(alertEventProducer, never()).send(any());
    }
}
