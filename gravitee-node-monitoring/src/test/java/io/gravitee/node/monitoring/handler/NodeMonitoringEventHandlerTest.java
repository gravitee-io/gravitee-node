/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.monitoring.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.node.api.infos.NodeStatus;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.gravitee.node.monitoring.monitor.probe.JvmProbe;
import io.gravitee.node.monitoring.monitor.probe.OsProbe;
import io.gravitee.node.monitoring.monitor.probe.ProcessProbe;
import io.gravitee.node.plugin.cluster.standalone.StandaloneClusterManager;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(value = { MockitoExtension.class, VertxExtension.class })
class NodeMonitoringEventHandlerTest {

    private static final String NODE_ID = "node#1";

    @Mock
    private Node node;

    @Mock
    private NodeMonitoringService nodeMonitoringService;

    private NodeMonitoringEventHandler cut;

    @BeforeEach
    public void beforeEach(Vertx vertx) throws Exception {
        when(node.id()).thenReturn("nodeId");
        StandaloneClusterManager clusterManager = new StandaloneClusterManager(vertx);
        clusterManager.start();
        cut = new NodeMonitoringEventHandler(vertx, clusterManager, new ObjectMapper(), node, nodeMonitoringService);
        cut.start();
    }

    @Test
    void should_handle_node_info_event(Vertx vertx) {
        final NodeInfos nodeInfos = new NodeInfos();
        nodeInfos.setEvaluatedAt(System.currentTimeMillis());
        nodeInfos.setStatus(NodeStatus.STARTED);
        nodeInfos.setId(NODE_ID);

        when(nodeMonitoringService.createOrUpdate(any())).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
        vertx.eventBus().publish(NodeInfosService.GIO_NODE_INFOS_BUS, nodeInfos);
        verify(nodeMonitoringService, timeout(500))
            .createOrUpdate(
                argThat(monitoring -> {
                    assertThat(monitoring.getNodeId()).isEqualTo("nodeId");
                    assertThat(monitoring.getEvaluatedAt().getTime()).isEqualTo(nodeInfos.getEvaluatedAt());
                    assertThat(monitoring.getType()).isEqualTo(Monitoring.NODE_INFOS);
                    return true;
                })
            );
    }

    @Test
    void should_handle_healtcheck_event(Vertx vertx) {
        final HealthCheck healthCheck = new HealthCheck(System.currentTimeMillis(), Map.of("test", Result.healthy("ok")));
        vertx.eventBus().publish(NodeHealthCheckService.GIO_NODE_HEALTHCHECK_BUS, healthCheck);

        verify(nodeMonitoringService, timeout(500))
            .createOrUpdate(
                argThat(monitoring -> {
                    assertThat(monitoring.getNodeId()).isEqualTo("nodeId");
                    assertThat(monitoring.getEvaluatedAt().getTime()).isEqualTo(healthCheck.getEvaluatedAt());
                    assertThat(monitoring.getType()).isEqualTo(Monitoring.HEALTH_CHECK);
                    return true;
                })
            );
    }

    @Test
    void should_handle_monitor_event(Vertx vertx) {
        final Monitor monitor = Monitor
            .on(NODE_ID)
            .at(System.currentTimeMillis())
            .os(OsProbe.getInstance().osInfo())
            .jvm(JvmProbe.getInstance().jvmInfo())
            .process(ProcessProbe.getInstance().processInfo())
            .build();
        vertx.eventBus().publish(NodeMonitorService.GIO_NODE_MONITOR_BUS, monitor);

        verify(nodeMonitoringService, timeout(500))
            .createOrUpdate(
                argThat(monitoring -> {
                    assertThat(monitoring.getNodeId()).isEqualTo("nodeId");
                    assertThat(monitoring.getEvaluatedAt().getTime()).isEqualTo(monitor.getTimestamp());
                    assertThat(monitoring.getType()).isEqualTo(Monitoring.MONITOR);
                    return true;
                })
            );
    }
}
