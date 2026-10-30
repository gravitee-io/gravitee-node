/*
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazelcast.core.HazelcastInstance;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastClusterConfigurationTest {

    private static final String REST_CLIENT_CA_STORE_TYPE_PROPERTY = "hazelcast.restclient.ca.storetype";

    private HazelcastClusterConfiguration configuration;

    @BeforeEach
    void setUp() {
        Node node = mock(Node.class);
        when(node.id()).thenReturn("node-id");
        when(node.hostname()).thenReturn("node-hostname");

        configuration = new HazelcastClusterConfiguration();
        ReflectionTestUtils.setField(configuration, "hazelcastConfigFilePath", "src/test/resources/cluster.xml");
        ReflectionTestUtils.setField(configuration, "hazelcastInstanceName", "test-cluster-hz-instance");
        ReflectionTestUtils.setField(configuration, "node", node);
    }

    @AfterEach
    void clearProperty() {
        System.clearProperty(REST_CLIENT_CA_STORE_TYPE_PROPERTY);
    }

    @Test
    void should_use_the_default_keystore_type_for_the_rest_client_ca_store() throws Exception {
        HazelcastInstance instance = configuration.clusterHazelcastInstance();
        try {
            assertThat(System.getProperty(REST_CLIENT_CA_STORE_TYPE_PROPERTY)).isEqualTo(KeyStoreUtils.DEFAULT_KEYSTORE_TYPE);
        } finally {
            instance.shutdown();
        }
    }

    @Test
    void should_keep_the_rest_client_ca_store_type_set_by_the_operator() throws Exception {
        System.setProperty(REST_CLIENT_CA_STORE_TYPE_PROPERTY, "BCFKS");

        HazelcastInstance instance = configuration.clusterHazelcastInstance();
        try {
            assertThat(System.getProperty(REST_CLIENT_CA_STORE_TYPE_PROPERTY)).isEqualTo("BCFKS");
        } finally {
            instance.shutdown();
        }
    }
}
