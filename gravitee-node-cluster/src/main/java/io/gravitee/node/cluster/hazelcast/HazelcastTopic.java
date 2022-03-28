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
package io.gravitee.node.cluster.hazelcast;

import com.hazelcast.topic.ITopic;
import io.gravitee.node.api.message.Message;
import io.gravitee.node.api.message.MessageConsumer;
import io.gravitee.node.api.message.Topic;
import java.util.UUID;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastTopic<T> implements Topic<T> {

    private final ITopic<T> iTopic;

    public HazelcastTopic(ITopic<T> iTopic) {
        this.iTopic = iTopic;
    }

    @Override
    public void publish(T event) {
        iTopic.publish(event);
    }

    @Override
    public UUID addMessageConsumer(MessageConsumer<T> messageConsumer) {
        return iTopic.addMessageListener(message -> messageConsumer.onMessage(new Message<>(iTopic.getName(), message.getMessageObject())));
    }

    @Override
    public boolean removeMessageConsumer(UUID uuid) {
        return iTopic.removeMessageListener(uuid);
    }
}
