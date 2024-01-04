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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Queue<T> {
    /**
     * Publish a new message on the current queue
     *
     * @param item the item to send
     * @throws IllegalStateException – if the element cannot be added at this time due to capacity restrictions
     */
    void add(T item);

    /**
     * Reactive version of {@link Queue#add(T)}. By default, execution will be done on IO schedulers.
     *
     * @param item the item to send
     * @return returns a {@code Completable} instance that completes in case of success
     */
    default Completable rxAdd(T item) {
        return Completable.fromRunnable(() -> this.add(item)).subscribeOn(Schedulers.io());
    }

    /**
     * Add a new listener on this queue. The given listener will be notified on any new message on the queue.
     * @param messageListener the listener to notify
     * @return the subscription identifier. Could be used to remove this listener.
     */
    String addMessageListener(final MessageListener<T> messageListener);

    /**
     * Reactive version of {@link Queue#addMessageListener(MessageListener)}. By default, execution will be done on IO schedulers.
     *
     * @param messageListener the listener to notify
     * @return a {@code Single} with the subscription identifier. Could be used to remove this listener.
     */
    default Single<String> rxAddMessageListener(final MessageListener<T> messageListener) {
        return Single.fromCallable(() -> this.addMessageListener(messageListener)).subscribeOn(Schedulers.io());
    }

    /**
     * Remove a listener on this queue from its subscription id.
     * @param subscriptionId the subscription id used to remove the listener
     * @return <code>true</code> if any listener has been removed, <code>false</code> otherwise.
     */
    boolean removeMessageListener(final String subscriptionId);

    /**
     * Reactive version of {@link Queue#removeMessageListener(String)}. By default, execution will be done on IO schedulers.
     *
     * @param subscriptionId the subscription id used to remove the listener
     * @return returns a {@code Completable} instance that completes in case of listener has been removed.
     */
    default Completable rxRemoveMessageListener(final String subscriptionId) {
        return Completable.fromRunnable(() -> this.removeMessageListener(subscriptionId)).subscribeOn(Schedulers.io());
    }
}
