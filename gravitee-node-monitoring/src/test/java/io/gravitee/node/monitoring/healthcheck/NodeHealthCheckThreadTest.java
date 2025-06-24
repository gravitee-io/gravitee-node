package io.gravitee.node.monitoring.healthcheck;

import static io.gravitee.node.monitoring.MonitoringConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.alert.api.event.Event;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.monitoring.DefaultProbeEvaluator;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.eventbus.MessageProducer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
class NodeHealthCheckThreadTest {

    protected static final String NODE_ID = "NODE_ID";

    @Mock
    private DefaultProbeEvaluator probeRegistry;

    @Mock
    private AlertEventProducer alertEventProducer;

    @Mock
    private Node node;

    @Mock
    private MessageProducer<HealthCheck> producer;

    @Mock
    private NodeHealthCheckService nodeHealthCheckService;

    private NodeHealthCheckThread cut;

    @BeforeEach
    void init() {
        cut = new NodeHealthCheckThread(probeRegistry, alertEventProducer, producer, node, nodeHealthCheckService);
    }

    @Test
    void should_produce_a_health_check_and_an_alert_event() {
        final Map<Probe, Result> probeResultMap = fakeProbeResults();

        final Set<String> orgIds = Set.of("ORG_ID");
        final Set<String> envIds = Set.of("ENV_ID");

        when(node.id()).thenReturn(NODE_ID);
        when(node.application()).thenReturn("APPLICATION");
        when(node.hostname()).thenReturn("HOSTNAME");
        when(node.metadata()).thenReturn(Map.of(Node.META_ORGANIZATIONS, orgIds, Node.META_ENVIRONMENTS, envIds));
        when(probeRegistry.evaluate()).thenReturn(CompletableFuture.completedFuture(probeResultMap));

        cut.run();

        verify(producer)
            .write(
                argThat(healthCheck -> {
                    assertThat(healthCheck.isHealthy()).isFalse();
                    assertThat(healthCheck.getEvaluatedAt()).isNotNull();
                    assertThat(healthCheck.getResults()).isNotNull();
                    assertThat(healthCheck.getResults().keySet())
                        .containsAll(probeResultMap.keySet().stream().filter(Probe::isVisibleByDefault).map(Probe::id).toList());

                    return true;
                })
            );

        verify(alertEventProducer)
            .send(
                argThat(event -> {
                    assertThat(event.type()).isEqualTo(NODE_HEALTHCHECK);
                    assertThat(event.properties().get(PROPERTY_NODE_ID)).isEqualTo(NODE_ID);
                    assertThat(event.properties().get(PROPERTY_NODE_HEALTHY)).isEqualTo("false");
                    assertThat(event.properties().get(PROPERTY_NODE_HOSTNAME)).isEqualTo("HOSTNAME");
                    assertThat(event.properties().get(PROPERTY_NODE_APPLICATION)).isEqualTo("APPLICATION");
                    assertThat(event.properties().get(Event.PROPERTY_ORGANIZATION)).isEqualTo("ORG_ID");
                    assertThat(event.properties().get(Event.PROPERTY_ENVIRONMENT)).isEqualTo("ENV_ID");

                    return true;
                })
            );
    }

    private Map<Probe, Result> fakeProbeResults() {
        Map<Probe, Result> probesMap = new HashMap<>();
        probesMap.put(new TestingProbe("http-server"), Result.healthy());
        probesMap.put(new TestingProbe("management-repository"), Result.unhealthy("Mock unhealthy"));
        probesMap.put(new TestingProbe("ratelimit-repository"), Result.healthy());
        probesMap.put(new TestingProbe("cpu", false), Result.healthy());

        return probesMap;
    }
}
