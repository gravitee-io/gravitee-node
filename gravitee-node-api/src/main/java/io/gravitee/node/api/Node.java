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
package io.gravitee.node.api;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.utils.UUID;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Node extends LifecycleComponent<Node> {
    String ID = UUID.toString(UUID.random());

    /**
     * Returns the node name in a human format
     *
     * @return The node name in a human format
     */
    String name();

    /**
     * Returns the node name in a technical format
     *
     * @return The node name in a technical format
     */
    String application();

    /**
     * Returns the node id.
     *
     * @return The node id.
     */
    default String id() {
        return ID;
    }

    default List<Class<? extends LifecycleComponent>> components() {
        return Collections.emptyList();
    }

    default String hostname() {
        return "";
    }

    default Map<String, Object> metadata() {
        return Collections.emptyMap();
    }
}
