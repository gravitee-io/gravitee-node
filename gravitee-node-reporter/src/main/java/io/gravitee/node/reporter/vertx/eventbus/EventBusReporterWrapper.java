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
package io.gravitee.node.reporter.vertx.eventbus;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class EventBusReporterWrapper implements Reporter, Handler<Message<Reportable>> {

    private static final String EVENT_BUS_ADDRESS = "node:metrics";
    private final Reporter reporter;
    private final Vertx vertx;

    public EventBusReporterWrapper(final Vertx vertx, final Reporter reporter) {
        this.vertx = vertx;
        this.reporter = reporter;
    }

    @Override
    public void report(Reportable reportable) {
        // Done by the event bus handler
        // See handle method
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return reporter.lifecycleState();
    }

    @Override
    public Reporter start() {
        vertx
            .executeBlocking(reporter::start)
            .onComplete(event -> {
                if (event.succeeded()) {
                    vertx.eventBus().consumer(EVENT_BUS_ADDRESS, EventBusReporterWrapper.this);
                } else {
                    log.error("Error while starting reporter", event.cause());
                }
            });

        return reporter;
    }

    @Override
    public Reporter stop() throws Exception {
        return reporter.stop();
    }

    @Override
    public void handle(Message<Reportable> reportableMsg) {
        Reportable reportable = reportableMsg.body();
        if (reporter.canHandle(reportable)) {
            reporter.report(reportable);
        }
    }
}
