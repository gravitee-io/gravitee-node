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
package io.gravitee.node.cluster.standalone;

import io.gravitee.node.api.message.Message;
import io.gravitee.node.api.message.Topic;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageConsumer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneTopic<T> implements Topic<T> {

  private final Vertx vertx;
  private final String topicName;
  Map<UUID, MessageConsumer<T>> consumerMap = new ConcurrentHashMap<>();

  public StandaloneTopic(Vertx vertx, String topicName) {
    this.vertx = vertx;
    this.topicName = topicName;
  }

  @Override
  public void publish(T event) {
    DeliveryOptions deliveryOptions = new DeliveryOptions();
    deliveryOptions.setCodecName(event.getClass().getName());

    vertx.eventBus().publish(topicName, event, deliveryOptions);
  }

  @Override
  public UUID addMessageConsumer(
    io.gravitee.node.api.message.MessageConsumer<T> messageConsumer
  ) {
    UUID uuid = io.gravitee.common.utils.UUID.random();

    MessageConsumer<T> vertxConsumer = vertx.eventBus().consumer(topicName);
    consumerMap.put(uuid, vertxConsumer);

    vertxConsumer.handler(
      event -> messageConsumer.onMessage(new Message<>(topicName, event.body()))
    );

    return uuid;
  }

  @Override
  public boolean removeMessageConsumer(UUID uuid) {
    if (!consumerMap.containsKey(uuid)) {
      return false;
    } else {
      Future<Void> unregister = consumerMap.get(uuid).unregister();

      if (unregister.succeeded()) {
        consumerMap.remove(uuid);
        return true;
      } else {
        return false;
      }
    }
  }
}
