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
package io.gravitee.node.monitoring.monitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.gravitee.node.monitoring.monitor.probe.JvmProbe;
import io.gravitee.node.monitoring.monitor.probe.OsProbe;
import io.gravitee.node.monitoring.monitor.probe.ProcessProbe;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class NodeMonitorManagementEndpoint implements ManagementEndpoint {

    private final ObjectMapper mapper;

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/monitor";
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        try {
            ObjectNode root = mapper.createObjectNode();

            JsonNode os = mapper.valueToTree(OsProbe.getInstance().osInfo());
            JsonNode jvm = mapper.valueToTree(JvmProbe.getInstance().jvmInfo());
            JsonNode process = mapper.valueToTree(ProcessProbe.getInstance().processInfo());

            root.set("os", os);
            root.set("jvm", jvm);
            root.set("process", process);

            response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            response.setChunked(true);
            response.setStatusCode(HttpStatusCode.OK_200);
            response.write(mapper.writeValueAsString(root));
        } catch (JsonProcessingException e) {
            log.error("Unexpected error while generating monitoring", e);
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        }

        response.end();
    }
}
