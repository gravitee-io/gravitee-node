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
package io.gravitee.node.cluster;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.MemberListener;
import java.util.Collection;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClusterManagerDelegate implements ClusterManager {

    private ClusterManager target;

    @Override
    public Collection<Member> getMembers() {
        return target.getMembers();
    }

    @Override
    public Member getLocalMember() {
        return target.getLocalMember();
    }

    @Override
    public boolean isMasterNode() {
        return target.isMasterNode();
    }

    @Override
    public boolean isStandalone() {
        return target.isStandalone();
    }

    @Override
    public void addMemberListener(MemberListener listener) {
        target.addMemberListener(listener);
    }

    @Override
    public void stop() {
        target.stop();
    }

    public void setTarget(ClusterManager target) {
        this.target = target;
    }
}
