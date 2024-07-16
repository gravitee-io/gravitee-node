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
package io.gravitee.node.management.http.vertx.endpoint;

import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Default management endpoint manager that holds a reference on all management endpoints that have been registered.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultManagementEndpointManager implements ManagementEndpointManager {

    private final List<ManagementEndpoint> managementEndpoints = new CopyOnWriteArrayList<>();
    private final List<Consumer<ManagementEndpoint>> onRegisteredlisteners = new CopyOnWriteArrayList<>();
    private final List<Consumer<ManagementEndpoint>> onUnregisteredlisteners = new CopyOnWriteArrayList<>();

    @Override
    public void onEndpointRegistered(Consumer<ManagementEndpoint> listener) {
        onRegisteredlisteners.add(listener);
        managementEndpoints.forEach(listener);
    }

    @Override
    public void onEndpointUnregistered(final Consumer<ManagementEndpoint> listener) {
        onUnregisteredlisteners.add(listener);
    }

    @Override
    public void register(ManagementEndpoint endpoint) {
        managementEndpoints.add(endpoint);
        onRegisteredlisteners.forEach(l -> l.accept(endpoint));
    }

    @Override
    public void unregister(final ManagementEndpoint endpoint) {
        if (managementEndpoints.remove(endpoint)) {
            onUnregisteredlisteners.forEach(l -> l.accept(endpoint));
        }
    }
}
