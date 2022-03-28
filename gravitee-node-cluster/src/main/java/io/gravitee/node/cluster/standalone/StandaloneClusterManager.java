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
package io.gravitee.node.cluster.standalone;

import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.MemberListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneClusterManager implements ClusterManager {

    private static final Member localMember = new Member() {
        @Override
        public String uuid() {
            return UUID.random().toString();
        }

        @Override
        public boolean master() {
            return true;
        }

        @Override
        public String host() {
            return "localhost";
        }

        @Override
        public Map<String, String> attributes() {
            return Collections.emptyMap();
        }

        @Override
        public Member attribute(String key, String value) {
            return null;
        }
    };

    @Override
    public Collection<Member> getMembers() {
        return Collections.singleton(localMember);
    }

    @Override
    public Member getLocalMember() {
        return localMember;
    }

    @Override
    public boolean isMasterNode() {
        return true;
    }

    @Override
    public void addMemberListener(MemberListener listener) {}

    @Override
    public void stop() {}
}
