package io.gravitee.node.cluster.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class ClusterEndpoint implements ManagementEndpoint {

    private final ClusterManager clusterManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/cluster";
    }

    @Override
    public void handle(RoutingContext context) {
        var response = context.response();

        try {
            response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            response.setChunked(true);
            response.setStatusCode(HttpStatusCode.OK_200);
            response.write(objectMapper.writeValueAsString(clusterManager.clusterInfo()));
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("An error occurred while handling the cluster info", jsonProcessingException);
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        }
        response.end();
    }
}
