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
package io.gravitee.node.api.cluster;

import io.gravitee.common.service.Service;
import io.gravitee.node.api.cluster.messaging.Queue;
import io.gravitee.node.api.cluster.messaging.Topic;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ClusterManager extends Service<ClusterManager> {
    /**
     * @return the unique cluster identifier
     */
    String clusterId();
    /**
     * @return the current list of members of this cluster
     */
    Set<Member> members();

    /**
     * @return the member attached to the local and current node instance
     */
    Member self();

    /**
     * Allow to add a new {@link MemberListener} which will be notified when members come and go.
     * @param listener the listener to be notified
     */
    void addMemberListener(final MemberListener listener);

    /**
     * Allow to remove an existing {@link MemberListener}. It won't be notified anymore.
     * @param listener the listener to be removed
     */
    void removeMemberListener(final MemberListener listener);

    /**
     * Return a {@link Topic<T>} used to publish or consume messages.
     * @param name the name used to retrieve the topic
     * @return a {@link Topic<T>}
     * @param <T> the type of content that will be published or consumed.
     */
    <T> Topic<T> topic(final String name);

    /**
     * Return a {@link Queue <T>} used to send or consume messages.
     * @param name the name used to retrieve the queue
     * @return a {@link Queue<T>}
     * @param <T> the type of content that will be published or consumed.
     */
    <T> Queue<T> queue(final String name);
}
