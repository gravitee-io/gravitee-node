package io.gravitee.node.cluster.healthcheck;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.vertx.core.Promise;
import java.util.concurrent.CompletionStage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@NoArgsConstructor
public class ClusterProbe implements Probe {

    @Autowired
    private ClusterManager clusterManager;

    @Override
    public String id() {
        return "cluster";
    }

    @Override
    public CompletionStage<Result> check() {
        Promise<Result> promise = Promise.promise();

        if (clusterManager.isRunning() && clusterManager.self().running()) {
            promise.complete(Result.healthy());
        } else {
            promise.complete(Result.unhealthy("Cluster is not running"));
        }

        return promise.future().toCompletionStage();
    }
}
