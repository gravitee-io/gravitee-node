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

import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.message.MessageProducer;
import io.gravitee.node.cluster.hazelcast.HazelcastCacheManager;
import io.gravitee.node.cluster.hazelcast.HazelcastClusterManager;
import io.gravitee.node.cluster.hazelcast.HazelcastMessageProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Conditional(HazelcastClusterConfiguration.ClusterModeEnabled.class)
public class HazelcastClusterConfiguration {

  @Value(
    "${cluster.hazelcast.config.path:${gravitee.home}/config/hazelcast.xml}"
  )
  private String hazelcastConfigFilePath;

  @Bean
  public HazelcastInstance distributedHazelcastInstance() throws Exception {
    // Force Hazelcast to use SLF4J before loading any HZ classes
    System.setProperty(ClusterProperty.LOGGING_TYPE.getName(), "slf4j");
    System.setProperty(ClusterProperty.SHUTDOWNHOOK_ENABLED.getName(), "false");

    FileSystemXmlConfig config = new FileSystemXmlConfig(
      hazelcastConfigFilePath
    );

    // Force the classloader used by Hazelcast to the container's classloader.
    config.setClassLoader(ClusterConfiguration.class.getClassLoader());

    config.setProperty(
      ClusterProperty.HEALTH_MONITORING_LEVEL.getName(),
      "OFF"
    );

    return Hazelcast.newHazelcastInstance(
      new FileSystemXmlConfig(hazelcastConfigFilePath)
    );
  }

  @Bean("hazelcastClusterManager")
  public ClusterManager hazelcastClusterManager() {
    return new HazelcastClusterManager();
  }

  @Bean("hazelcastCacheManager")
  public CacheManager hazelcastCacheManager() {
    return new HazelcastCacheManager();
  }

  @Bean("hazelcastMessageProducer")
  public MessageProducer hazelcastMessageProducer() {
    return new HazelcastMessageProducer();
  }

  public static class ClusterModeEnabled implements Condition {

    @Override
    public boolean matches(
      ConditionContext conditionContext,
      AnnotatedTypeMetadata annotatedTypeMetadata
    ) {
      return "true".equals(
          conditionContext
            .getEnvironment()
            .getProperty("gravitee.cluster.enabled")
        );
    }
  }
}
