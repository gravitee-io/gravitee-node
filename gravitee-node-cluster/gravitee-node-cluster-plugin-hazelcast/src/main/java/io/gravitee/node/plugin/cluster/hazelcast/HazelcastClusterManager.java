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

import com.hazelcast.core.HazelcastInstance;

/**
 * @deprecated Moved to {@link io.gravitee.node.plugin.cluster.hazelcast.provider.HazelcastClusterManager}
 *     in 9.0 when the {@code cluster-plugin-hazelcast} plugin was merged into
 *     {@code cluster-plugin-hazelcast-provider} to keep the {@link HazelcastInstance} in a single
 *     plugin classloader. Update your imports.
 */
@Deprecated(since = "9.0", forRemoval = true)
public class HazelcastClusterManager extends io.gravitee.node.plugin.cluster.hazelcast.provider.HazelcastClusterManager {

    public HazelcastClusterManager(HazelcastInstance hazelcastInstance) {
        super(hazelcastInstance);
    }
}
