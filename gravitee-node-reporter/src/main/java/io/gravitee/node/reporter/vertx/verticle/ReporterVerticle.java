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
package io.gravitee.node.reporter.vertx.verticle;

import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.reporter.ReporterService;
import io.gravitee.node.reporter.vertx.eventbus.ReportableMessageCodec;
import io.gravitee.reporter.api.Reportable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.tracing.TracingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReporterVerticle extends AbstractVerticle implements ReporterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporterVerticle.class);

    private static final String EVENT_BUS_ADDRESS = "node:metrics";

    private MessageProducer<Reportable> producer;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        // Register specific codec
        vertx.eventBus().registerCodec(new ReportableMessageCodec());

        producer =
            vertx
                .eventBus()
                .<Reportable>publisher(
                    EVENT_BUS_ADDRESS,
                    new DeliveryOptions().setCodecName(ReportableMessageCodec.CODEC_NAME).setTracingPolicy(TracingPolicy.IGNORE)
                );

        // By default we report node monitor data.
        vertx.eventBus().<Monitor>localConsumer("gio:node:monitor", event -> producer.write(event.body()));

        promise.complete();
    }

    @Override
    public void stop(Promise<Void> promise) throws Exception {
        if (producer != null) {
            producer.close(
                event -> {
                    if (event.succeeded()) {
                        LOGGER.debug("Reporter publisher has been closed successfully.");
                        promise.complete();
                    } else {
                        promise.fail(event.cause());
                    }
                }
            );
        } else {
            promise.complete();
        }
    }

    public void report(Reportable reportable) {
        if (producer != null) {
            producer.write(reportable);
        }
    }
}
