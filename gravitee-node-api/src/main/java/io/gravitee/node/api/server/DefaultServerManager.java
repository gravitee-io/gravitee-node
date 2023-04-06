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
package io.gravitee.node.api.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultServerManager implements ServerManager {

    private final List<Server<?>> servers = new ArrayList<>();

    @Override
    public void register(Server<?> server) {
        servers.add(server);
    }

    @Override
    public void unregister(Server<?> server) {
        servers.remove(server);
    }

    @Override
    public List<Server<?>> servers() {
        return Collections.unmodifiableList(servers);
    }

    @Override
    public <T extends Server<?>> List<T> servers(Class<T> serverClazz) {
        return servers
            .stream()
            .filter(server -> serverClazz.isAssignableFrom(server.getClass()))
            .map(serverClazz::cast)
            .collect(Collectors.toList());
    }
}
