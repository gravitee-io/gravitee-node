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
package io.gravitee.node.plugin.cluster.hazelcast;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.MemberListener;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.node.plugin.cluster.hazelcast.messaging.HazelcastTopic;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class HazelcastClusterManager extends AbstractService<ClusterManager> implements ClusterManager, MembershipListener {

    private final HazelcastInstance hazelcastInstance;

    private final Set<MemberListener> memberListeners = new HashSet<>();

    @Override
    protected void doStart() {
        hazelcastInstance.getCluster().addMembershipListener(this);
    }

    @Override
    protected void doStop() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    @Override
    public Set<Member> members() {
        return hazelcastInstance
            .getCluster()
            .getMembers()
            .stream()
            .map(member -> new HazelcastMember(member, isPrimaryMember(member)))
            .collect(Collectors.toSet());
    }

    @Override
    public Member self() {
        com.hazelcast.cluster.Member localMember = hazelcastInstance.getCluster().getLocalMember();
        return new HazelcastMember(localMember, isPrimaryMember(localMember));
    }

    @Override
    public void addMemberListener(final MemberListener listener) {
        memberListeners.add(listener);
    }

    @Override
    public void removeMemberListener(final MemberListener listener) {
        memberListeners.remove(listener);
    }

    @Override
    public <T> Topic<T> topic(final String name) {
        ITopic<T> iTopic = hazelcastInstance.getReliableTopic(name);
        return new HazelcastTopic<>(iTopic);
    }

    @Override
    public void memberAdded(final MembershipEvent event) {
        log.info("A node joined the cluster: {}", event);
        com.hazelcast.cluster.Member eventMember = event.getMember();
        Member newMember = new HazelcastMember(eventMember, isPrimaryMember(eventMember));
        memberListeners.forEach(listener -> listener.onMemberAdded(newMember));
    }

    @Override
    public void memberRemoved(final MembershipEvent event) {
        log.info("A node leaved the cluster: {}", event);
        com.hazelcast.cluster.Member eventMember = event.getMember();
        Member removeMember = new HazelcastMember(eventMember, isPrimaryMember(eventMember));
        memberListeners.forEach(listener -> listener.onMemberRemoved(removeMember));
    }

    private boolean isPrimaryMember(com.hazelcast.cluster.Member member) {
        com.hazelcast.cluster.Member firstMemberAsPrimary = hazelcastInstance.getCluster().getMembers().iterator().next();
        return member != null && member.equals(firstMemberAsPrimary);
    }
}
