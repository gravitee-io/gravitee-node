package io.gravitee.node.cluster.healthcheck;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.cluster.ClusterManager;
import java.util.concurrent.ExecutionException;
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
class ClusterProbeTest {

    private ClusterProbe cut;

    @Mock
    private ClusterManager clusterManager;

    @BeforeEach
    void setUp() {
        cut = new ClusterProbe(clusterManager);
    }

    @Test
    @SneakyThrows
    public void should_be_healthy_when_cluster_is_running() {
        when(clusterManager.isRunning()).thenReturn(true);

        var result = cut.check().toCompletableFuture().get();

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    @SneakyThrows
    public void should_be_unhealthy_when_cluster_is_not_running() {
        when(clusterManager.isRunning()).thenReturn(false);

        var result = cut.check().toCompletableFuture().get();

        assertThat(result.isHealthy()).isFalse();
    }
}
