package io.gravitee.node.monitoring.infos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class NodeInfosServiceTest {

    protected static final String NODE_ID = "NODE_ID";

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private Node node;

    private final MockEnvironment environment = new MockEnvironment();

    @Test
    @SneakyThrows
    void should_start_the_service() {
        environment.setProperty("http.port", "8888");
        environment.setProperty("tags", "tag1,tag2");
        environment.setProperty("metadata[0]_id", "#id");
        environment.setProperty("metadata[1]_foo", "bar");
        when(node.id()).thenReturn(NODE_ID);
        when(node.application()).thenReturn("APPLICATION");
        mockPluginRegistry();

        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<NodeInfos> nodeInfosRef = new AtomicReference<>();
        Vertx vertx = Vertx.vertx();
        vertx
            .eventBus()
            .localConsumer(
                NodeInfosService.GIO_NODE_INFOS_BUS,
                event -> {
                    nodeInfosRef.set((NodeInfos) event.body());
                    latch.countDown();
                }
            );
        final NodeInfosService cut = new NodeInfosService(pluginRegistry, environment, node, vertx);

        cut.doStart();

        latch.await(100, TimeUnit.MILLISECONDS);
        NodeInfos nodeInfos = nodeInfosRef.get();
        assertThat(nodeInfos).isNotNull();
        assertThat(nodeInfos.getId()).isEqualTo(NODE_ID);
        assertThat(nodeInfos.getApplication()).isEqualTo("APPLICATION");
        assertThat(nodeInfos.getPort()).isEqualTo(8888);
        assertThat(nodeInfos.getTags()).isEqualTo(List.of("tag1", "tag2"));
        assertThat(nodeInfos.getPluginInfos()).isNotEmpty();
        assertThat(nodeInfos.getVersion()).isNotNull();
        assertThat(nodeInfos.getJdkVersion()).isNotNull();
        assertThat(nodeInfos.getEvaluatedAt()).isNotNull();
        assertThat(nodeInfos.getHostname()).isNotNull();
        assertThat(nodeInfos.getIp()).isNotNull();
        assertThat(nodeInfos.getMetadata()).isEqualTo(Map.of("id", "#id", "foo", "bar"));
    }

    private void mockPluginRegistry() {
        final io.gravitee.plugin.core.api.Plugin alertPlugin = mock(io.gravitee.plugin.core.api.Plugin.class);
        final PluginManifest alertPluginManifest = mock(PluginManifest.class);
        lenient().when(alertPlugin.type()).thenReturn("alert");
        lenient().when(alertPlugin.manifest()).thenReturn(alertPluginManifest);
        final io.gravitee.plugin.core.api.Plugin notifierPlugin = mock(io.gravitee.plugin.core.api.Plugin.class);
        final PluginManifest notifierPluginManifest = mock(PluginManifest.class);
        lenient().when(notifierPlugin.type()).thenReturn("notifier");
        lenient().when(notifierPlugin.manifest()).thenReturn(notifierPluginManifest);
        lenient().when(pluginRegistry.plugins()).thenReturn(List.of(alertPlugin, notifierPlugin));
    }
}
