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

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.node.api.infos.NodeStatus;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.monitor.probe.JvmProbe;
import io.gravitee.node.monitoring.monitor.probe.OsProbe;
import io.gravitee.node.monitoring.monitor.probe.ProcessProbe;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusteredNodeMonitoringEventHandlerTest {

  private static final String NODE_ID = "node#1";

  @Mock
  protected Vertx vertx;

  @Mock
  protected Node node;

  @Mock
  protected NodeMonitoringService nodeMonitoringService;

  @Mock
  protected ObjectMapper objectMapper;

  @Mock
  private ClusterManager clusterManager;

  @Mock
  private HazelcastInstance hazelcastInstance;

  @Mock
  private ITopic<HealthCheck> distributedHealthCheckTopic;

  @Mock
  private ITopic<Monitor> distributedMonitorTopic;

  @Mock
  private ITopic<NodeInfos> distributedNodeInfosTopic;

  private ClusteredNodeMonitoringEventHandler cut;

  @Before
  public void before() {
    cut =
      new ClusteredNodeMonitoringEventHandler(
        vertx,
        objectMapper,
        node,
        nodeMonitoringService,
        clusterManager,
        hazelcastInstance
      );
    cut.distributedHealthCheckTopic = distributedHealthCheckTopic;
    cut.distributedMonitorTopic = distributedMonitorTopic;
    cut.distributedNodeInfosTopic = distributedNodeInfosTopic;
  }

  @Test
  public void handleNodeInfosMessage() throws JsonProcessingException {
    final Message<NodeInfos> message = mock(Message.class);

    final NodeInfos nodeInfos = new NodeInfos();
    nodeInfos.setEvaluatedAt(System.currentTimeMillis());
    nodeInfos.setStatus(NodeStatus.STARTED);
    nodeInfos.setId(NODE_ID);

    when(message.body()).thenReturn(nodeInfos);

    cut.handleNodeInfosMessage(message);

    verify(distributedNodeInfosTopic).publish(nodeInfos);
  }

  @Test
  public void handleMonitorMessageNot() throws JsonProcessingException {
    final Message<Monitor> message = mock(Message.class);

    final Monitor monitor = Monitor
      .on(NODE_ID)
      .at(System.currentTimeMillis())
      .os(OsProbe.getInstance().osInfo())
      .jvm(JvmProbe.getInstance().jvmInfo())
      .process(ProcessProbe.getInstance().processInfo())
      .build();

    when(message.body()).thenReturn(monitor);

    cut.handleMonitorMessage(message);

    verify(distributedMonitorTopic).publish(monitor);
  }

  @Test
  public void handleHealthCheckMessage() throws JsonProcessingException {
    final Message<HealthCheck> message = mock(Message.class);

    final HashMap<String, Result> results = new HashMap<>();
    results.put("test", Result.healthy("ok"));
    final HealthCheck healthCheck = new HealthCheck(
      System.currentTimeMillis(),
      results
    );

    when(message.body()).thenReturn(healthCheck);

    cut.handleHealthCheckMessage(message);

    verify(distributedHealthCheckTopic).publish(healthCheck);
  }
}
