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
package io.gravitee.node.cluster.spring;

import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.message.MessageProducer;
import io.gravitee.node.cluster.ClusterManagerDelegate;
import io.gravitee.node.cluster.ClusterService;
import io.gravitee.node.cluster.MessageProducerDelegate;
import io.gravitee.node.cluster.cache.CacheManagerDelegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ClusterConfiguration {

    @Bean
    public ClusterService clusterService() {
        return new ClusterService();
    }

    @Bean
    public ClusterManager clusterManager() {
        return new ClusterManagerDelegate();
    }

    @Bean
    public CacheManager cacheManager() {
        return new CacheManagerDelegate();
    }

    @Bean
    public MessageProducer messageProducer() {
        return new MessageProducerDelegate();
    }
}
