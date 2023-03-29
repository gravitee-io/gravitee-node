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
package io.gravitee.node.api.cluster.messaging;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Topic<T> {
    /**
     * Publish a new message on the current topic
     * @param message the message to publish
     */
    void publish(T message);

    /**
     * Add a new listener on this topic. The given listener will be notified on any new message on the topic.
     * @param messageListener the listener to notify
     * @return the subscription identifier. Could be used to remove this listener.
     */
    String addMessageListener(final MessageListener<T> messageListener);

    /**
     * Remove a listener on this topic from its subscription id.
     * @param subscriptionId the subscription id used to remove the listener
     * @return <code>true</code> if any listener has been removed, <code>false</code> otherwise.
     */
    boolean removeMessageListener(final String subscriptionId);
}
