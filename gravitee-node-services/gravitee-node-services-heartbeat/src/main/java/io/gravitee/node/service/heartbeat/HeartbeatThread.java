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
package io.gravitee.node.service.heartbeat;

import io.gravitee.node.api.heartbeat.Event;
import io.vertx.core.eventbus.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HeartbeatThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatThread.class);

    private final MessageProducer<Event> producer;
    private final Event event;

    HeartbeatThread(final MessageProducer<Event> producer, final Event event) {
        this.producer = producer;
        this.event = event;
    }

    @Override
    public void run() {
        LOGGER.debug("Run heartbeat for node at {}", new Date());

        // Update heartbeat timestamp
        event.setUpdatedAt(new Date());

        producer.write(event);
    }
}