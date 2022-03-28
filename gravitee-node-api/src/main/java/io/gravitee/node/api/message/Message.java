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
package io.gravitee.node.api.message;

import java.util.EventObject;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Message<T> extends EventObject {

    private T messageObject;

    /**
     * Constructs a prototypical Event.
     *
     * @param topicName the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public Message(String topicName, T messageObject) {
        super(topicName);
        this.messageObject = messageObject;
    }

    public T getMessageObject() {
        return messageObject;
    }

    public void setMessageObject(T messageObject) {
        this.messageObject = messageObject;
    }
}
