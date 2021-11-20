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
import io.gravitee.node.cache.standalone.StandaloneCacheManager;
import io.gravitee.node.cluster.standalone.StandaloneClusterManager;
import io.gravitee.node.cluster.standalone.StandaloneMessageProducer;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Conditional(StandaloneClusterConfiguration.StandaloneModeEnabled.class)
public class StandaloneClusterConfiguration {

  @Bean("standaloneClusterManager")
  public ClusterManager standaloneClusterManager() {
    return new StandaloneClusterManager();
  }

  @Bean("standaloneCacheManager")
  public CacheManager standaloneCacheManager() {
    return new StandaloneCacheManager();
  }

  @Bean("standaloneMessageProducer")
  public MessageProducer standaloneMessageProducer() {
    return new StandaloneMessageProducer();
  }

  public static class StandaloneModeEnabled implements ConfigurationCondition {

    @Override
    public boolean matches(
      ConditionContext conditionContext,
      AnnotatedTypeMetadata annotatedTypeMetadata
    ) {
      return !conditionContext
        .getEnvironment()
        .getProperty("gravitee.cluster.enabled", Boolean.class, false);
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }
}
