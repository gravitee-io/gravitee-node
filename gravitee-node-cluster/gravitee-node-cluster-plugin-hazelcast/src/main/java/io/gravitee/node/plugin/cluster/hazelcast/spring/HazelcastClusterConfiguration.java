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
package io.gravitee.node.plugin.cluster.hazelcast.spring;

import com.hazelcast.core.HazelcastInstance;
import io.gravitee.node.plugin.cluster.hazelcast.HazelcastClusterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link HazelcastInstance} bean is owned by the {@code hazelcast-provider} plugin and
 * registered in the main application context — we autowire it here rather than construct it,
 * to guarantee a single instance shared with any {@link io.gravitee.node.api.cluster.DistributedMapProvider}
 * consumer.
 */
@Configuration
public class HazelcastClusterConfiguration {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Bean
    public HazelcastClusterManager hazelcastClusterManager() {
        return new HazelcastClusterManager(hazelcastInstance);
    }
}
