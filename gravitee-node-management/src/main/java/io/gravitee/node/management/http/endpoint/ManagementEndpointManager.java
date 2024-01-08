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
package io.gravitee.node.management.http.endpoint;

import java.util.function.Consumer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ManagementEndpointManager {
    /**
     * Register a listener that will be called each time a management endpoint is registered.
     * If endpoints are register before the listener, the listener will be notified with all the existing management endpoints.
     *
     * @param listener the listener that will be called each time a management endpoint is registered.
     */
    void onEndpointRegistered(Consumer<ManagementEndpoint> listener);

    /**
     * Register a new management endpoint. The endpoint will be immediately propagated to all the registered listeners.
     *
     * @param endpoint the endpoint to register
     */
    void register(ManagementEndpoint endpoint);
}
