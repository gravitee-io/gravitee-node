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
package io.gravitee.node.jetty.healthcheck;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import java.util.concurrent.CompletionStage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP Probe used to check the Jetty Http Server is listening.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JettyHttpServerProbe implements Probe {

    @Autowired
    private Configuration configuration;

    @Autowired
    private Vertx vertx;

    private NetClient client;

    public JettyHttpServerProbe() {}

    public JettyHttpServerProbe(Configuration configuration, Vertx vertx) {
        this.configuration = configuration;
        this.vertx = vertx;
    }

    @Override
    public String id() {
        return "jetty-http-server";
    }

    @Override
    public CompletionStage<Result> check() {
        Promise<Result> promise = Promise.promise();

        netClient()
            .connect(port(), host())
            .onComplete(res -> {
                if (res.succeeded()) {
                    promise.complete(Result.healthy());
                    res.result().close();
                } else {
                    promise.complete(Result.unhealthy(res.cause()));
                }
            });

        return promise.future().toCompletionStage();
    }

    /**
     * Vert.x holds on to every client it creates until the owner it was created from is closed, so a client per
     * check would accumulate for as long as the node runs. A single client serves them all and is released when
     * Vert.x itself stops.
     */
    private synchronized NetClient netClient() {
        if (client == null) {
            client = vertx.createNetClient(new NetClientOptions().setConnectTimeout(500));
        }
        return client;
    }

    private int port() {
        return configuration.getProperty("jetty.port", Integer.class, 8093);
    }

    private String host() {
        return configuration.getProperty("jetty.host", "localhost");
    }
}
