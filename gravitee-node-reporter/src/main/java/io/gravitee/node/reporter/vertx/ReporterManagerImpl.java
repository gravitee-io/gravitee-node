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
package io.gravitee.node.reporter.vertx;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.reporter.ReporterManager;
import io.gravitee.node.reporter.vertx.eventbus.EventBusReporterWrapper;
import io.gravitee.node.reporter.vertx.verticle.ReporterVerticle;
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.gravitee.reporter.api.Reporter;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Collection;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ReporterManagerImpl extends AbstractService<ReporterManager> implements ReporterManager {

    @Autowired
    private Vertx vertx;

    private String deploymentId;

    private final Collection<Reporter> reporters = new ArrayList<>();

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        vertx.deployVerticle(
            SpringVerticleFactory.VERTICLE_PREFIX + ':' + ReporterVerticle.class.getName(),
            event -> {
                if (event.failed()) {
                    log.error("Reporter service can not be started", event.cause());
                } else {
                    if (!reporters.isEmpty()) {
                        for (Reporter reporter : reporters) {
                            try {
                                log.debug("Pre-starting reporter: {}", reporter);
                                reporter.preStart();
                            } catch (Exception ex) {
                                log.error("Unexpected error while pre-starting reporter", ex);
                            }
                        }

                        for (Reporter reporter : reporters) {
                            try {
                                log.info("Starting reporter: {}", reporter);
                                reporter.start();
                            } catch (Exception ex) {
                                log.error("Unexpected error while starting reporter", ex);
                            }
                        }

                        for (Reporter reporter : reporters) {
                            try {
                                log.debug("Port-starting reporter: {}", reporter);
                                reporter.postStart();
                            } catch (Exception ex) {
                                log.error("Unexpected error while post-starting reporter", ex);
                            }
                        }
                    } else {
                        log.info("\tThere is no reporter to start");
                    }
                }

                deploymentId = event.result();
            }
        );
    }

    @Override
    public void register(Reporter reporter) {
        reporters.add(new EventBusReporterWrapper(vertx, reporter));
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (deploymentId != null) {
            vertx.undeploy(
                deploymentId,
                event -> {
                    for (Reporter reporter : reporters) {
                        try {
                            log.debug("Pre-stopping reporter: {}", reporter);
                            reporter.preStop();
                        } catch (Exception ex) {
                            log.error("Unexpected error while pre-stopping reporter", ex);
                        }
                    }

                    for (Reporter reporter : reporters) {
                        try {
                            log.info("Stopping reporter: {}", reporter);
                            reporter.stop();
                        } catch (Exception ex) {
                            log.error("Unexpected error while stopping reporter", ex);
                        }
                    }

                    for (Reporter reporter : reporters) {
                        try {
                            log.debug("Post-stopping reporter: {}", reporter);
                            reporter.postStop();
                        } catch (Exception ex) {
                            log.error("Unexpected error while post-stopping reporter", ex);
                        }
                    }
                }
            );
        }
    }

    @Override
    protected String name() {
        return "Reporter service";
    }
}
