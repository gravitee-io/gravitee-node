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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthCheckManagementEndpoint implements ManagementEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    NodeHealthCheckManagementEndpoint.class
  );

  private NodeHealthCheckThread registry;

  final ObjectMapper objectMapper;

  public static final String PROBE_FILTER = "probes";

  public NodeHealthCheckManagementEndpoint() {
    objectMapper = DatabindCodec.prettyMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Override
  public HttpMethod method() {
    return HttpMethod.GET;
  }

  @Override
  public String path() {
    return "/health";
  }

  @Override
  public void handle(RoutingContext ctx) {
    Map<Probe, Result> probes = registry
      .getResults()
      .entrySet()
      .stream()
      .filter(
        entry ->
          ctx.queryParams().contains(PROBE_FILTER)
            ? ctx.queryParams().get(PROBE_FILTER).contains(entry.getKey().id())
            : entry.getKey().isVisibleByDefault()
      )
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    boolean healthyProbe = probes.values().stream().allMatch(Result::isHealthy);

    HttpServerResponse response = ctx.response();
    response.setStatusCode(
      healthyProbe
        ? HttpStatusCode.OK_200
        : HttpStatusCode.INTERNAL_SERVER_ERROR_500
    );
    response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    response.setChunked(true);

    Map<String, Result> results = probes
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(
          probeResultEntry -> probeResultEntry.getKey().id(),
          Map.Entry::getValue
        )
      );

    try {
      final ObjectMapper objectMapper = DatabindCodec.prettyMapper();
      response.write(objectMapper.writeValueAsString(results));
    } catch (JsonProcessingException e) {
      LOGGER.warn("Unable to encode health check result into json.", e);
    }

    // End the response
    response.end();
  }

  public void setRegistry(NodeHealthCheckThread registry) {
    this.registry = registry;
  }
}
