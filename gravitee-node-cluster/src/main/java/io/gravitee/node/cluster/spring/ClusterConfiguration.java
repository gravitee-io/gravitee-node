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
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.cluster.ClusterService;
import io.gravitee.node.cluster.hazelcast.HazelcastClusterManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ClusterConfiguration {

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

  @Bean
  public ClusterManager clusterManager() {
    return new HazelcastClusterManager();
  }

  @Bean
  public ClusterService clusterService() {
    return new ClusterService();
  }
}
