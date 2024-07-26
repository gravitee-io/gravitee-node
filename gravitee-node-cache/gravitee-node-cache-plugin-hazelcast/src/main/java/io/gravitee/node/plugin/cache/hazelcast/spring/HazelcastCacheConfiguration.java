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
package io.gravitee.node.plugin.cache.hazelcast.spring;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.FileSystemYamlConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.node.api.Node;
import java.io.FileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class HazelcastCacheConfiguration {

    @Value("${cache.hazelcast.config-path:${gravitee.home}/config/hazelcast-cache.xml}")
    private String hazelcastConfigFilePath;

    @Value("${cache.hazelcast.instance-name:gio-cache-hz-instance}")
    private String hazelcastInstanceName;

    @Autowired
    private Node node;

    @Bean
    public HazelcastInstance cacheHazelcastInstance() throws FileNotFoundException {
        // Force Hazelcast to use SLF4J before loading any HZ classes
        System.setProperty(ClusterProperty.LOGGING_TYPE.getName(), "slf4j");
        System.setProperty(ClusterProperty.SHUTDOWNHOOK_ENABLED.getName(), "false");

        Config config = fromFilePath(hazelcastConfigFilePath);
        if (!config.getClusterName().contains("cache")) {
            config.setClusterName(config.getClusterName() + "-cache");
        }
        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        config.setInstanceName(hazelcastInstanceName);

        MemberAttributeConfig memberAttributeConfig = new MemberAttributeConfig();
        memberAttributeConfig.setAttribute("gio_node_id", node.id());
        memberAttributeConfig.setAttribute("gio_node_hostname", node.hostname());
        config.setMemberAttributeConfig(memberAttributeConfig);

        return Hazelcast.newHazelcastInstance(config);
    }

    private Config fromFilePath(String filePath) throws FileNotFoundException {
        if (filePath.endsWith("xml")) {
            return new FileSystemXmlConfig(hazelcastConfigFilePath);
        } else if (filePath.endsWith("yaml") || filePath.endsWith("yml")) {
            return new FileSystemYamlConfig(hazelcastConfigFilePath);
        }

        throw new IllegalArgumentException("Only xml or yaml file supported for Hazelcast configuration");
    }
}
