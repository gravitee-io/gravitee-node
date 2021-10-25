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

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.MemberListener;
import io.gravitee.node.cluster.member.NodeMember;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastClusterManager implements InitializingBean, ClusterManager, MembershipListener {

    private final Logger LOGGER = LoggerFactory.getLogger(HazelcastClusterManager.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private final Set<MemberListener> memberListeners = new HashSet<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        hazelcastInstance.getCluster().addMembershipListener(this);
    }

    private boolean isMaster(com.hazelcast.cluster.Member member) {
        com.hazelcast.cluster.Member master = hazelcastInstance.getCluster().getMembers().iterator().next();
        return member.equals(master);
    }

    @Override
    public Collection<Member> getMembers() {
        return hazelcastInstance
            .getCluster()
            .getMembers()
            .stream()
            .map(
                new Function<com.hazelcast.cluster.Member, Member>() {
                    @Override
                    public Member apply(com.hazelcast.cluster.Member member) {
                        return new NodeMember(member, isMaster(member));
                    }
                }
            )
            .collect(Collectors.toSet());
    }

    @Override
    public Member getLocalMember() {
        return new NodeMember(hazelcastInstance.getCluster().getLocalMember(), isMaster(hazelcastInstance.getCluster().getLocalMember()));
    }

    @Override
    public boolean isMasterNode() {
        return isMaster(hazelcastInstance.getCluster().getLocalMember());
    }

    @Override
    public void addMemberListener(MemberListener listener) {
        memberListeners.add(listener);
    }

    @Override
    public void memberAdded(MembershipEvent event) {
        LOGGER.info("A node join the cluster: {}", event);
        com.hazelcast.cluster.Member master = hazelcastInstance.getCluster().getMembers().iterator().next();
        Member newMember = new NodeMember(event.getMember(), master.equals(event.getMember()));

        memberListeners.forEach(listener -> listener.memberAdded(newMember));
    }

    @Override
    public void memberRemoved(MembershipEvent event) {
        LOGGER.info("A node leave the cluster: {}", event);
        com.hazelcast.cluster.Member master = hazelcastInstance.getCluster().getMembers().iterator().next();
        Member newMember = new NodeMember(event.getMember(), master.equals(event.getMember()));

        memberListeners.forEach(listener -> listener.memberRemoved(newMember));
    }
}
