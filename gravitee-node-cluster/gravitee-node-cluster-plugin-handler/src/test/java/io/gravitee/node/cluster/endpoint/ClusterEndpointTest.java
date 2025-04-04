package io.gravitee.node.cluster.endpoint;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.cluster.ClusterInfo;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClusterEndpointTest {

    private ClusterEndpoint cut;

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private RoutingContext routingContext;

    @Mock
    private HttpServerResponse response;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(routingContext.response()).thenReturn(response);

        cut = new ClusterEndpoint(clusterManager);
    }

    @Test
    @SneakyThrows
    void should_return_cluster_info() {
        var member = new TestMember();
        var clusterInfo = new ClusterInfo("clusterId", true, member, Set.of(member));
        when(clusterManager.clusterInfo()).thenReturn(clusterInfo);
        String expectedOutput = objectMapper.writeValueAsString(clusterInfo);

        cut.handle(routingContext);

        verify(response).setStatusCode(200);
        verify(response).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(response).write(expectedOutput);
    }

    private static class TestMember implements Member {

        @Override
        public String id() {
            return "testMemberId";
        }

        @Override
        public boolean primary() {
            return true;
        }

        @Override
        public boolean self() {
            return true;
        }

        @Override
        public String host() {
            return "host";
        }

        @Override
        public String version() {
            return "1.4.5";
        }

        @Override
        public Boolean running() {
            return true;
        }

        @Override
        public Map<String, String> attributes() {
            return Map.of();
        }

        @Override
        public Member attribute(String key, String value) {
            return this;
        }
    }
}
