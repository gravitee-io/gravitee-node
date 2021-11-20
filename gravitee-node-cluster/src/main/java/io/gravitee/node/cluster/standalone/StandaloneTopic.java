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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;
import java.util.ArrayList;
import java.util.List;
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
  private static final List<String> messageCodecs = new ArrayList<>();

  public StandaloneTopic(Vertx vertx, String topicName) {
    this.vertx = vertx;
    this.topicName = topicName;

    if (!messageCodecs.contains(topicName)) {
      messageCodecs.add(topicName);

      vertx
        .eventBus()
        .registerCodec(
          new MessageCodec<T, T>() {
            // Will not be used for local transformations
            @Override
            public void encodeToWire(Buffer buffer, T o) {}

            // Will not be used for local transformations
            @Override
            public T decodeFromWire(int pos, Buffer buffer) {
              return null;
            }

            @Override
            public T transform(T o) {
              return o;
            }

            @Override
            public String name() {
              return topicName;
            }

            @Override
            public byte systemCodecID() {
              return -1;
            }
          }
        );
    }
  }

  @Override
  public void publish(T event) {
    DeliveryOptions deliveryOptions = new DeliveryOptions();
    deliveryOptions.setCodecName(topicName);

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
