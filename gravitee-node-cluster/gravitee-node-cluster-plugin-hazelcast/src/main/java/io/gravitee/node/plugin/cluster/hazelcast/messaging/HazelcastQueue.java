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
package io.gravitee.node.plugin.cluster.hazelcast.messaging;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.node.api.cluster.messaging.MessageListener;
import io.gravitee.node.api.cluster.messaging.Queue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class HazelcastQueue<T> implements Queue<T> {

    private final IQueue<T> iQueue;
    private final Map<String, QueuePollingThread<T>> queuePollingThreads = new ConcurrentHashMap<>();

    @Override
    public void add(T item) {
        iQueue.add(item);
    }

    @Override
    public String addMessageListener(final MessageListener<T> messageListener) {
        String subscriptionId = io.gravitee.common.utils.UUID.random().toString();
        QueuePollingThread<T> queuePollingThread = new QueuePollingThread<>(iQueue, messageListener);
        queuePollingThread.start();
        queuePollingThreads.put(subscriptionId, queuePollingThread);
        return subscriptionId;
    }

    @Override
    public boolean removeMessageListener(final String subscriptionId) {
        QueuePollingThread<T> queuePollingThread = queuePollingThreads.remove(subscriptionId);
        if (queuePollingThread != null) {
            queuePollingThread.terminate();
        }
        return true;
    }

    @RequiredArgsConstructor
    @CustomLog
    public static class QueuePollingThread<T> extends Thread {

        private final IQueue<T> queue;
        private final MessageListener<T> messageListener;
        private volatile boolean running;

        @Override
        public void run() {
            running = true;

            while (running) {
                try {
                    final T item = queue.poll(100, TimeUnit.MILLISECONDS);

                    if (item != null) {
                        messageListener.onMessage(new Message<>(queue.getName(), item));
                    }
                } catch (HazelcastInstanceNotActiveException e) {
                    log.info("Hazelcast is not active, stop polling queue '{}'.", queue.getName());
                    break;
                } catch (Exception e) {
                    log.warn("Polling hazelcast queue '{}' encountered an error.", queue.getName(), e);
                }
            }
        }

        public void terminate() {
            this.running = false;
            try {
                // Make sure current polling is terminated
                this.join(1_000);
            } catch (InterruptedException e) {
                log.warn("Polling hazelcast queue '{}' encountered an error when terminating", queue.getName(), e);
            }
        }
    }
}
