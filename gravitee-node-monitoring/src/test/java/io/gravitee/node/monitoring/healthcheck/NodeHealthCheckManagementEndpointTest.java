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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.Map;
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
    private NodeHealthCheckThread probeStatusRegistry;

    @Mock
    private RoutingContext routingContext;

    @Mock
    private MultiMap queryParams;

    private NodeHealthCheckManagementEndpoint nodeHealthCheckManagementEndpoint;

    @Mock
    private HttpServerResponse httpServerResponse;

    @BeforeEach
    public void beforeEach() {
        nodeHealthCheckManagementEndpoint = new NodeHealthCheckManagementEndpoint();
        nodeHealthCheckManagementEndpoint.setRegistry(probeStatusRegistry);

        when(routingContext.response()).thenReturn(httpServerResponse);
    }

    @Test
    void should_not_filter_all_healthy() {
        Map<Probe, Result> probeResultMap = fakeProbeResults(true);
        when(probeStatusRegistry.getResults()).thenReturn(probeResultMap);
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
        when(probeStatusRegistry.getResults()).thenReturn(probeResultMap);
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
        when(probeStatusRegistry.getResults()).thenReturn(probeResultMap);
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(any())).thenReturn(true);
        when(queryParams.get(any())).thenReturn("ratelimit-repository,management-repository");

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
        when(probeStatusRegistry.getResults()).thenReturn(probeResultMap);
        when(routingContext.queryParams()).thenReturn(queryParams);
        when(queryParams.contains(any())).thenReturn(true);
        when(queryParams.get(any())).thenReturn("ratelimit-repository,management-repository,cpu");

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

    private static class TestingProbe implements Probe {

        private final String id;
        private boolean isVisibleByDefault = true;

        public TestingProbe(String id) {
            this.id = id;
        }

        public TestingProbe(String id, boolean isVisibleByDefault) {
            this.id = id;
            this.isVisibleByDefault = isVisibleByDefault;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isVisibleByDefault() {
            return this.isVisibleByDefault;
        }

        @Override
        public CompletableFuture<Result> check() {
            return null;
        }
    }
}
