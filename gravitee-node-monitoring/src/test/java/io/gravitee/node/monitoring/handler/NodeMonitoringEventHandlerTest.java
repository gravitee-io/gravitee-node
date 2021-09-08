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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.HealthCheck;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.node.api.infos.NodeStatus;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.monitoring.NodeMonitoringService;
import io.gravitee.node.monitoring.monitor.probe.JvmProbe;
import io.gravitee.node.monitoring.monitor.probe.OsProbe;
import io.gravitee.node.monitoring.monitor.probe.ProcessProbe;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeMonitoringEventHandlerTest {

  private static final String NODE_ID = "node#1";

  @Mock
  protected Vertx vertx;

  @Mock
  protected Node node;

  @Mock
  protected NodeMonitoringService nodeMonitoringService;

  @Mock
  protected ObjectMapper objectMapper;

  private NodeMonitoringEventHandler cut;

  @Before
  public void before() {
    cut =
      new NodeMonitoringEventHandler(
        vertx,
        objectMapper,
        node,
        nodeMonitoringService
      );
  }

  @Test
  public void handleNodeInfosMessage() throws JsonProcessingException {
    final Message<NodeInfos> message = mock(Message.class);

    final NodeInfos nodeInfos = new NodeInfos();
    nodeInfos.setEvaluatedAt(System.currentTimeMillis());
    nodeInfos.setStatus(NodeStatus.STARTED);
    nodeInfos.setId(NODE_ID);

    when(node.id()).thenReturn(NODE_ID);
    when(message.body()).thenReturn(nodeInfos);
    when(objectMapper.writeValueAsString(nodeInfos)).thenReturn("expected");

    ArgumentCaptor<Monitoring> captor = ArgumentCaptor.forClass(
      Monitoring.class
    );

    when(nodeMonitoringService.createOrUpdate(any(Monitoring.class)))
      .thenAnswer(i -> Single.just(i.getArgument(0)));
    cut.handleNodeInfosMessage(message);

    verify(nodeMonitoringService).createOrUpdate(captor.capture());

    final Monitoring monitoring = captor.getValue();

    assertEquals(NODE_ID, monitoring.getNodeId());
    assertEquals(
      nodeInfos.getEvaluatedAt(),
      monitoring.getEvaluatedAt().getTime()
    );
    assertEquals(Monitoring.NODE_INFOS, monitoring.getType());
    assertEquals("expected", monitoring.getPayload());
  }

  @Test
  public void handleMonitorMessage() throws JsonProcessingException {
    final Message<Monitor> message = mock(Message.class);

    final Monitor monitor = Monitor
      .on(NODE_ID)
      .at(System.currentTimeMillis())
      .os(OsProbe.getInstance().osInfo())
      .jvm(JvmProbe.getInstance().jvmInfo())
      .process(ProcessProbe.getInstance().processInfo())
      .build();

    when(node.id()).thenReturn(NODE_ID);
    when(message.body()).thenReturn(monitor);
    when(objectMapper.writeValueAsString(monitor)).thenReturn("expected");

    ArgumentCaptor<Monitoring> captor = ArgumentCaptor.forClass(
      Monitoring.class
    );

    when(nodeMonitoringService.createOrUpdate(any(Monitoring.class)))
      .thenAnswer(i -> Single.just(i.getArgument(0)));
    cut.handleMonitorMessage(message);

    verify(nodeMonitoringService).createOrUpdate(captor.capture());

    final Monitoring monitoring = captor.getValue();

    assertEquals(NODE_ID, monitoring.getNodeId());
    assertEquals(monitor.getTimestamp(), monitoring.getEvaluatedAt().getTime());
    assertEquals(Monitoring.MONITOR, monitoring.getType());
    assertEquals("expected", monitoring.getPayload());
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

    when(node.id()).thenReturn(NODE_ID);
    when(message.body()).thenReturn(healthCheck);
    when(objectMapper.writeValueAsString(healthCheck)).thenReturn("expected");

    ArgumentCaptor<Monitoring> captor = ArgumentCaptor.forClass(
      Monitoring.class
    );

    when(nodeMonitoringService.createOrUpdate(any(Monitoring.class)))
      .thenAnswer(i -> Single.just(i.getArgument(0)));
    cut.handleHealthCheckMessage(message);

    verify(nodeMonitoringService).createOrUpdate(captor.capture());

    final Monitoring monitoring = captor.getValue();

    assertEquals(NODE_ID, monitoring.getNodeId());
    assertEquals(
      healthCheck.getEvaluatedAt(),
      monitoring.getEvaluatedAt().getTime()
    );
    assertEquals(Monitoring.HEALTH_CHECK, monitoring.getType());
    assertEquals("expected", monitoring.getPayload());
  }
}
