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

import io.gravitee.node.api.cluster.Member;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@RequiredArgsConstructor
public class HazelcastMember implements Member {

    private final com.hazelcast.cluster.Member member;
    private final boolean primary;
    private Boolean running;

    @Override
    public String id() {
        return member.getUuid().toString();
    }

    @Override
    public boolean primary() {
        return primary;
    }

    @Override
    public boolean self() {
        return member.localMember();
    }

    @Override
    public String host() {
        return member.getAddress().getHost();
    }

    @Override
    public String version() {
        return member.getVersion().toString();
    }

    @Override
    public Boolean running() {
        return running;
    }

    @Override
    public Map<String, String> attributes() {
        return member.getAttributes();
    }

    @Override
    public Member attribute(String key, String value) {
        member.getAttributes().put(key, value);
        return this;
    }
}
