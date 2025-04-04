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

import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.cluster.Member;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneMember implements Member {

    private final String localId = UUID.random().toString();
    private final Map<String, String> attributes = new HashMap<>();

    @Override
    public String id() {
        return localId;
    }

    @Override
    public boolean primary() {
        return true;
    }

    @Override
    public boolean self() {
        return true;
    }

    @Override
    public String host() {
        return "localhost";
    }

    @Override
    public String version() {
        return "standalone";
    }

    @Override
    public Boolean running() {
        return true;
    }

    @Override
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public Member attribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }
}
