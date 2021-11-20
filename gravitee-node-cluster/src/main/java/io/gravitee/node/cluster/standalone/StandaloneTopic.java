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
import io.gravitee.node.api.message.MessageConsumer;
import io.gravitee.node.api.message.Topic;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneTopic<T> implements Topic<T> {

  private final String topicName;
  Map<UUID, MessageConsumer<T>> consumerMap = new ConcurrentHashMap<>();

  public StandaloneTopic(String topicName) {
    this.topicName = topicName;
  }

  @Override
  public void publish(T event) {
    consumerMap
      .values()
      .forEach(consumer -> consumer.onMessage(new Message<>(topicName, event)));
  }

  @Override
  public UUID addMessageConsumer(MessageConsumer<T> messageConsumer) {
    UUID uuid = io.gravitee.common.utils.UUID.random();
    consumerMap.put(uuid, messageConsumer);

    return uuid;
  }

  @Override
  public boolean removeMessageConsumer(UUID uuid) {
    if (!consumerMap.containsKey(uuid)) {
      return false;
    } else {
      consumerMap.remove(uuid);
      return true;
    }
  }
}
