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

import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.reporter.ReporterService;
import io.gravitee.node.reporter.health.ReporterHealth;
import io.gravitee.node.reporter.vertx.eventbus.ReportableMessageCodec;
import io.gravitee.reporter.api.Reportable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReporterVerticle extends AbstractVerticle implements ReporterService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReporterVerticle.class);

    private static final String EVENT_BUS_ADDRESS = "node:metrics";

    private MessageProducer<Reportable> producer;

    @Override
    public void start() throws Exception {
        // Register specific codec
        vertx.eventBus().registerCodec(new ReportableMessageCodec());

        producer = vertx.eventBus()
                .<Reportable>publisher(
                    EVENT_BUS_ADDRESS,
                    new DeliveryOptions()
                            .setCodecName(ReportableMessageCodec.CODEC_NAME))
                .exceptionHandler(
                        throwable -> LOGGER.error("Unexpected error while sending a reportable element", throwable));
    }

    @Override
    public void stop() throws Exception {
        if (producer != null) {
            producer.close();
        }
    }

    public void report(Reportable reportable) {
        if (producer != null) {
            producer.write(reportable);
        }
    }

    @Override
    public CompletableFuture<Result> health() {
        CompletableFuture<Result> future = new CompletableFuture<>();
        if (producer == null) {
            future.complete(Result.unhealthy("Message producer is not started"));
        } else {
            producer.send(new ReporterHealth(System.currentTimeMillis()), ar -> {
                if (ar.succeeded()) {
                    future.complete(Result.healthy());
                } else {
                    LOGGER.error("Unexpected error while checking reporter health", ar.cause());
                    future.complete(Result.unhealthy("No answer from the reporter : %s", ar.cause()));
                }
            });
        }
        return future;
    }
}
