package io.gravitee.node.monitoring.monitor;

import static io.gravitee.node.monitoring.MonitoringConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.alert.api.event.Event;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.eventbus.MessageProducer;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class NodeMonitorThreadTest {

    protected static final String NODE_ID = "NODE_ID";

    @Mock
    private MessageProducer<Monitor> producer;

    @Mock
    private Node node;

    @Mock
    private AlertEventProducer alertEventProducer;

    private NodeMonitorThread cut;

    @BeforeEach
    void init() {
        cut = new NodeMonitorThread(producer, node, alertEventProducer);
    }

    @Test
    void should_produce_a_monitor() {
        when(node.id()).thenReturn(NODE_ID);
        cut.run();

        verify(producer)
            .write(
                argThat(monitor -> {
                    assertThat(monitor.getNodeId()).isEqualTo(NODE_ID);
                    assertThat(monitor.getJvm()).isNotNull();
                    assertThat(monitor.getOs()).isNotNull();
                    assertThat(monitor.getProcess()).isNotNull();

                    return true;
                })
            );
    }

    @Test
    void should_produce_a_monitor_and_send_alert_event_producers_are_available() {
        final Set<String> orgIds = Set.of("ORG_ID");
        final Set<String> envIds = Set.of("ENV_ID");

        when(node.id()).thenReturn(NODE_ID);
        when(node.application()).thenReturn("APPLICATION");
        when(node.hostname()).thenReturn("HOSTNAME");
        when(node.metadata()).thenReturn(Map.of(Node.META_ORGANIZATIONS, orgIds, Node.META_ENVIRONMENTS, envIds));
        when(alertEventProducer.isEmpty()).thenReturn(false);

        cut.run();

        verify(producer).write(any(Monitor.class));
        verify(alertEventProducer)
            .send(
                argThat(event -> {
                    assertThat(event.type()).isEqualTo(NODE_HEARTBEAT);
                    assertThat(event.properties().get(PROPERTY_NODE_ID)).isEqualTo(NODE_ID);
                    assertThat(event.properties().get(PROPERTY_NODE_HOSTNAME)).isEqualTo("HOSTNAME");
                    assertThat(event.properties().get(PROPERTY_NODE_APPLICATION)).isEqualTo("APPLICATION");
                    assertThat(event.properties().get(Event.PROPERTY_ORGANIZATION)).isEqualTo("ORG_ID");
                    assertThat(event.properties().get(Event.PROPERTY_ENVIRONMENT)).isEqualTo("ENV_ID");

                    assertThat(event.properties().keySet()).contains("os.cpu.percent", "process.fd.open", "jvm.uptime");
                    return true;
                })
            );
    }

    @Test
    void should_catch_any_exception() {
        when(producer.write(any(Monitor.class))).thenThrow(new RuntimeException("Mock exception"));

        cut.run();
    }
}
