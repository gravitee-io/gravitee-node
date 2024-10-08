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
package io.gravitee.node.monitoring.healthcheck;

import static io.gravitee.node.monitoring.healthcheck.NodeHealthCheckManagementEndpoint.PROBE_FILTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.ProbeEvaluator;
import io.gravitee.node.api.healthcheck.Result;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NodeHealthCheckManagementEndpointTest {

    @Mock
    private ProbeEvaluator probeEvaluator;

    @Mock
    private RoutingContext routingContext;

    @Mock
    private MultiMap queryParams;

    private NodeHealthCheckManagementEndpoint nodeHealthCheckManagementEndpoint;

    @Mock
    private HttpServerResponse httpServerResponse;

    @BeforeEach
    public void beforeEach() {
        nodeHealthCheckManagementEndpoint =
            new NodeHealthCheckManagementEndpoint(
                probeEvaluator,
                new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).setSerializationInclusion(JsonInclude.Include.NON_NULL)
            );

        when(routingContext.response()).thenReturn(httpServerResponse);
    }

    @Test
    void should_not_filter_all_healthy() {
        Map<Probe, Result> probeResultMap = fakeProbeResults(true);

        when(probeEvaluator.evaluate(null)).thenReturn(CompletableFuture.completedFuture(probeResultMap));
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(any())).thenReturn(false);

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> writeCaptor = ArgumentCaptor.forClass(String.class);

        nodeHealthCheckManagementEndpoint.handle(routingContext);

        verify(httpServerResponse).setStatusCode(statusCaptor.capture());
        assertThat((int) statusCaptor.getValue()).isEqualTo(HttpStatusCode.OK_200);
        verify(httpServerResponse).write(writeCaptor.capture());

        String expected =
            "{\n" +
            "  \"ratelimit-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"management-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"http-server\" : {\n" +
            "    \"healthy\" : true\n" +
            "  }\n" +
            "}";

        assertThat(writeCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    void should_not_filter_one_unhealthy_healthy() {
        Map<Probe, Result> probeResultMap = fakeProbeResults(false);
        when(probeEvaluator.evaluate(null)).thenReturn(CompletableFuture.completedFuture(probeResultMap));
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(any())).thenReturn(false);

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> writeCaptor = ArgumentCaptor.forClass(String.class);

        nodeHealthCheckManagementEndpoint.handle(routingContext);

        verify(httpServerResponse).setStatusCode(statusCaptor.capture());
        assertThat((int) statusCaptor.getValue()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        verify(httpServerResponse).write(writeCaptor.capture());

        String expected =
            "{\n" +
            "  \"ratelimit-repository\" : {\n" +
            "    \"healthy\" : false\n" +
            "  },\n" +
            "  \"management-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"http-server\" : {\n" +
            "    \"healthy\" : true\n" +
            "  }\n" +
            "}";

        assertThat(writeCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    void should_filter_all_healthy() {
        Map<Probe, Result> probeResultMap = fakeProbeResults(true);
        when(probeEvaluator.evaluate(Set.of("ratelimit-repository", "management-repository")))
            .thenReturn(CompletableFuture.completedFuture(probeResultMap));
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(PROBE_FILTER)).thenReturn(true);
        when(queryParams.getAll(PROBE_FILTER)).thenReturn(List.of("ratelimit-repository,management-repository"));

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> writeCaptor = ArgumentCaptor.forClass(String.class);

        nodeHealthCheckManagementEndpoint.handle(routingContext);

        verify(httpServerResponse).setStatusCode(statusCaptor.capture());
        assertThat((int) statusCaptor.getValue()).isEqualTo(HttpStatusCode.OK_200);
        verify(httpServerResponse).write(writeCaptor.capture());

        String expected =
            "{\n" +
            "  \"ratelimit-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"management-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  }\n" +
            "}";

        assertThat(writeCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    void should_filter_by_probe_id() {
        Map<Probe, Result> probeResultMap = fakeProbeResults(false);
        when(probeEvaluator.evaluate(Set.of("ratelimit-repository", "management-repository", "cpu")))
            .thenReturn(CompletableFuture.completedFuture(probeResultMap));
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(PROBE_FILTER)).thenReturn(true);
        when(queryParams.getAll(PROBE_FILTER)).thenReturn(List.of("ratelimit-repository,management-repository,cpu"));

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> writeCaptor = ArgumentCaptor.forClass(String.class);

        nodeHealthCheckManagementEndpoint.handle(routingContext);

        verify(httpServerResponse).setStatusCode(statusCaptor.capture());
        assertThat((int) statusCaptor.getValue()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        verify(httpServerResponse).write(writeCaptor.capture());

        String expected =
            "{\n" +
            "  \"ratelimit-repository\" : {\n" +
            "    \"healthy\" : false\n" +
            "  },\n" +
            "  \"management-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"cpu\" : {\n" +
            "    \"healthy\" : true\n" +
            "  }\n" +
            "}";

        assertThat(writeCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    void should_return_unhealthy_when_filtering_including_unknown_probe_id() {
        Map<Probe, Result> probeResultMap = fakeProbeResults(true);
        when(probeEvaluator.evaluate(Set.of("ratelimit-repository", "management-repository", "cpu", "unknown")))
            .thenReturn(CompletableFuture.completedFuture(probeResultMap));
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(PROBE_FILTER)).thenReturn(true);
        when(queryParams.getAll(PROBE_FILTER)).thenReturn(List.of("ratelimit-repository,management-repository,cpu,unknown"));

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> writeCaptor = ArgumentCaptor.forClass(String.class);

        nodeHealthCheckManagementEndpoint.handle(routingContext);

        verify(httpServerResponse).setStatusCode(statusCaptor.capture());
        assertThat((int) statusCaptor.getValue()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        verify(httpServerResponse).write(writeCaptor.capture());

        String expected =
            "{\n" +
            "  \"ratelimit-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"management-repository\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"cpu\" : {\n" +
            "    \"healthy\" : true\n" +
            "  },\n" +
            "  \"unknown\" : {\n" +
            "    \"healthy\" : false,\n" +
            "    \"message\" : \"probe not found\"\n" +
            "  }\n" +
            "}";

        assertThat(writeCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    void should_return_unhealthy_when_filtering_with_only_unknown_probe_id() {
        when(probeEvaluator.evaluate(Set.of("unknown"))).thenReturn(CompletableFuture.completedFuture(Map.of()));
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(PROBE_FILTER)).thenReturn(true);
        when(queryParams.getAll(PROBE_FILTER)).thenReturn(List.of("unknown"));

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> writeCaptor = ArgumentCaptor.forClass(String.class);

        nodeHealthCheckManagementEndpoint.handle(routingContext);

        verify(httpServerResponse).setStatusCode(statusCaptor.capture());
        assertThat((int) statusCaptor.getValue()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        verify(httpServerResponse).write(writeCaptor.capture());

        String expected =
            "{\n" + "  \"unknown\" : {\n" + "    \"healthy\" : false,\n" + "    \"message\" : \"probe not found\"\n" + "  }\n" + "}";

        assertThat(writeCaptor.getValue()).isEqualTo(expected);
    }

    private Map<Probe, Result> fakeProbeResults(boolean allHealthy) {
        Map<Probe, Result> probesMap = new HashMap<>();
        probesMap.put(new TestingProbe("http-server"), mockResult(true));
        probesMap.put(new TestingProbe("management-repository"), mockResult(true));
        probesMap.put(new TestingProbe("ratelimit-repository"), mockResult(allHealthy));
        probesMap.put(new TestingProbe("cpu", false), mockResult(true));

        return probesMap;
    }

    private Result mockResult(boolean isHealthy) {
        return isHealthy ? Result.healthy() : Result.unhealthy((String) null);
    }
}
