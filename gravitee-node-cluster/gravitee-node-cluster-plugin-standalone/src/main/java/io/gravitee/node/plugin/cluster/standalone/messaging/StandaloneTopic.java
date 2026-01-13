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
package io.gravitee.node.plugin.cluster.standalone.messaging;

import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.node.api.cluster.messaging.MessageListener;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageConsumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneTopic<T> implements Topic<T> {

    private final Map<String, MessageConsumer<T>> consumerMap = new ConcurrentHashMap<>();
    private final Vertx vertx;
    private final String topicName;
    private final DeliveryOptions deliveryOptions;

    public StandaloneTopic(final Vertx vertx, final String topicName) {
        this.vertx = vertx;
        this.topicName = topicName;
        this.deliveryOptions = new DeliveryOptions().setCodecName(StandaloneMessageCodec.STANDALONE_CODEC_NAME);
    }

    @Override
    public void publish(T event) {
        vertx.eventBus().publish(topicName, event, deliveryOptions);
    }

    @Override
    public String addMessageListener(final MessageListener<T> messageListener) {
        String subscriptionId = io.gravitee.common.utils.UUID.random().toString();

        MessageConsumer<T> vertxConsumer = vertx
            .eventBus()
            .<T>localConsumer(topicName)
            .handler(event ->
                vertx.executeBlocking(() -> {
                    messageListener.onMessage(new Message<>(topicName, event.body()));
                    return null;
                })
            );
        consumerMap.put(subscriptionId, vertxConsumer);

        return subscriptionId;
    }

    @Override
    public boolean removeMessageListener(final String subscriptionId) {
        if (consumerMap.containsKey(subscriptionId)) {
            return consumerMap.get(subscriptionId).unregister().onSuccess(event -> consumerMap.remove(subscriptionId)).succeeded();
        }
        return false;
    }
}
