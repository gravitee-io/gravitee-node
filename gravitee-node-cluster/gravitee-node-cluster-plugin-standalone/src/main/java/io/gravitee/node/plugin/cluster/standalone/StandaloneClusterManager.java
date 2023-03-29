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
package io.gravitee.node.plugin.cluster.standalone;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.MemberListener;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.node.plugin.cluster.standalone.messaging.StandaloneMessageCodec;
import io.gravitee.node.plugin.cluster.standalone.messaging.StandaloneTopic;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class StandaloneClusterManager extends AbstractService<ClusterManager> implements ClusterManager {

    private static final Member LOCAL_MEMBER = new StandaloneMember();
    private final Map<String, Topic<?>> topicsByName = new ConcurrentHashMap<>();
    private final Vertx vertx;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        vertx.eventBus().registerCodec(new StandaloneMessageCodec());
    }

    @Override
    public Set<Member> members() {
        return Set.of(LOCAL_MEMBER);
    }

    @Override
    public Member self() {
        return LOCAL_MEMBER;
    }

    @Override
    public void addMemberListener(MemberListener listener) {
        // Nothing to do here as no member can be added to a Standalone cluster
    }

    @Override
    public void removeMemberListener(final MemberListener listener) {
        // Nothing to do here as no member can be removed to a Standalone cluster
    }

    @Override
    public <T> Topic<T> topic(final String name) {
        return (Topic<T>) topicsByName.computeIfAbsent(name, key -> new StandaloneTopic<>(vertx, name));
    }
}
