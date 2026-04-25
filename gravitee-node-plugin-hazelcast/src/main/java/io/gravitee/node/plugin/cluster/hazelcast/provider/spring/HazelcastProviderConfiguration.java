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
package io.gravitee.node.plugin.cluster.hazelcast.provider.spring;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.FileSystemYamlConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.BuildInfoProvider;
import com.hazelcast.spi.properties.ClusterProperty;
import com.hazelcast.version.Version;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.DistributedMapProvider;
import io.gravitee.node.plugin.cluster.hazelcast.provider.HazelcastClusterManager;
import io.gravitee.node.plugin.cluster.hazelcast.provider.HazelcastDistributedMapProvider;
import java.io.FileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Owns the embedded Hazelcast instance and exposes it via {@link DistributedMapProvider},
 * {@link HazelcastClusterManager} (for {@code cluster.type=hazelcast}), and the raw instance itself.
 * Keeping all three concerns in the same plugin guarantees they share the same classloader — and
 * therefore the same {@link HazelcastInstance} class — which is required for Spring autowiring to
 * resolve the shared instance across downstream consumers.
 */
@Configuration
public class HazelcastProviderConfiguration {

    @Value("${cluster.hazelcast.config-path:${gravitee.home}/config/hazelcast-cluster.xml}")
    private String hazelcastConfigFilePath;

    @Value("${cluster.hazelcast.instance-name:gio-cluster-hz-instance}")
    private String hazelcastInstanceName;

    // WARNING: This option was introduced as a temporary fix for upgrading Hazelcast from v5.3 to v5.5.
    // Reference: https://github.com/hazelcast/hazelcast/issues/26486
    @Value("${cluster.hazelcast.cluster-name-versioning:true}")
    private boolean hazelcastClusterNameVersioning;

    @Autowired
    private Node node;

    @Bean(destroyMethod = "shutdown")
    @Lazy
    public HazelcastInstance clusterHazelcastInstance() throws FileNotFoundException {
        System.setProperty(ClusterProperty.LOGGING_TYPE.getName(), "slf4j");
        System.setProperty(ClusterProperty.SHUTDOWNHOOK_ENABLED.getName(), "false");

        Config config = fromFilePath(hazelcastConfigFilePath);
        if (!config.getClusterName().contains("cluster")) {
            config.setClusterName(config.getClusterName() + "-cluster-manager");
        }

        if (hazelcastClusterNameVersioning) {
            Version hzVersion = Version.of(BuildInfoProvider.getBuildInfo().getVersion());
            config.setClusterName(config.getClusterName() + "-hz" + hzVersion.getMajor() + hzVersion.getMinor());
        }

        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        config.setInstanceName(hazelcastInstanceName);

        MemberAttributeConfig memberAttributeConfig = new MemberAttributeConfig();
        memberAttributeConfig.setAttribute("gio_node_id", node.id());
        memberAttributeConfig.setAttribute("gio_node_hostname", node.hostname());
        config.setMemberAttributeConfig(memberAttributeConfig);

        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public DistributedMapProvider distributedMapProvider(@Lazy final HazelcastInstance hazelcastInstance) {
        return new HazelcastDistributedMapProvider(hazelcastInstance);
    }

    /**
     * {@link HazelcastClusterManager} takes the live {@link HazelcastInstance} in its constructor,
     * so creating this bean boots Hazelcast. Keep it {@code @Lazy} so Hazelcast only starts if the
     * plugin handler actually fetches this bean — which it does exclusively when
     * {@code cluster.type=hazelcast}. Under any other cluster type, this bean is never resolved and
     * Hazelcast stays dormant.
     */
    @Bean
    @Lazy
    public HazelcastClusterManager hazelcastClusterManager(final HazelcastInstance hazelcastInstance) {
        return new HazelcastClusterManager(hazelcastInstance);
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
