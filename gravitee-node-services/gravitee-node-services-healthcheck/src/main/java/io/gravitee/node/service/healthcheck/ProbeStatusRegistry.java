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
package io.gravitee.node.service.healthcheck;

import io.gravitee.alert.api.event.DefaultEvent;
import io.gravitee.alert.api.event.Event;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.Handler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProbeStatusRegistry implements Handler<Long> {

    private static final String NODE_HEALTHCHECK = "NODE_HEALTHCHECK";

    private static final String PROPERTY_NODE_HOSTNAME = "node.hostname";
    private static final String PROPERTY_NODE_APPLICATION = "node.application";
    private static final String PROPERTY_NODE_ID = "node.id";
    private static final String PROPERTY_NODE_HEALTHY = "node.healthy";
    private static final String PROPERTY_PROBE_SUFFIX = "node.probe.";

    @Autowired
    private AlertEventProducer eventProducer;

    @Autowired
    private Node node;

    private final Map<Probe, Result> results;

    public ProbeStatusRegistry(List<Probe> probes) {
        this.results = probes.stream().collect(Collectors.toMap(probe -> probe, probe -> Result.notReady()));
    }

    @Override
    public void handle(Long tick) {
        for (Map.Entry<Probe, Result> probe : results.entrySet()) {
            try {
                probe.getKey().check().thenAccept(probe::setValue);
            } catch (Exception ex) {
                ex.printStackTrace();
                probe.setValue(Result.unhealthy(ex));
            }
        }

        sendAlert();
    }

    private void sendAlert() {
        DefaultEvent.Builder builder = Event.now().type(NODE_HEALTHCHECK);

        builder.property(PROPERTY_NODE_ID, node.id());
        builder.property(PROPERTY_NODE_HOSTNAME, node.hostname());
        builder.property(PROPERTY_NODE_APPLICATION, node.application());
        builder.property(PROPERTY_NODE_HEALTHY, results.values().stream().allMatch(Result::isHealthy) ?  "true" : "false");

        results.forEach(new BiConsumer<Probe, Result>() {
            @Override
            public void accept(Probe probe, Result result) {
                builder.property(PROPERTY_PROBE_SUFFIX + probe.id(), result.isHealthy());
                if (! result.isHealthy()) {
                    builder.property(PROPERTY_PROBE_SUFFIX + probe.id() + ".message", result.getMessage());
                }
            }
        });

        eventProducer.send(builder.build());
    }

    public Map<Probe, Result> getResults() {
        return results;
    }
}
