/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graviteesource.services.runtimesecrets.grant;

import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.grant.Grant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrantRegistry {

    private final Map<String, Grant> grants = new ConcurrentHashMap<>();

    public void register(String id, Grant grant) {
        grants.put(id, grant);
    }

    public void unregister(DiscoveryContext... contexts) {
        if (contexts != null) {
            Arrays.stream(contexts).map(DiscoveryContext::id).map(UUID::toString).forEach(grants::remove);
        }
    }

    public Grant get(String token) {
        return grants.get(token);
    }
}
