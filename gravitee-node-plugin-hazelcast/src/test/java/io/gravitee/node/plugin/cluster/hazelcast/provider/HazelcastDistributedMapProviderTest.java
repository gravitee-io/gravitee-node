/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.node.plugin.cluster.hazelcast.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.node.api.cluster.DistributedMap;
import io.gravitee.node.api.cluster.DistributedMapProvider;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastDistributedMapProviderTest {

    private HazelcastInstance hazelcast;
    private DistributedMapProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        Config config = new FileSystemXmlConfig("src/test/resources/cluster.xml");
        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        config.setInstanceName("test-hz-distributed-map-" + System.nanoTime());
        hazelcast = Hazelcast.newHazelcastInstance(config);
        provider = new HazelcastDistributedMapProvider(hazelcast);
    }

    @AfterEach
    void tearDown() {
        hazelcast.shutdown();
    }

    @Test
    void shares_state_across_calls_with_same_name() {
        provider.<String, String>get("shared").put("k", "v", 60_000);
        assertThat(provider.<String, String>get("shared").get("k")).isEqualTo("v");
    }

    @Test
    void isolates_state_between_names() {
        DistributedMap<String, String> a = provider.get("a");
        DistributedMap<String, String> b = provider.get("b");
        a.put("k", "v", 60_000);
        assertThat(b.get("k")).isNull();
    }

    @Test
    void expires_entry_after_ttl() {
        DistributedMap<String, String> map = provider.get("ttl");
        map.put("k", "v", 100);
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(map.get("k")).isNull());
    }

    @Test
    void lock_blocks_second_thread_until_unlock() throws InterruptedException {
        DistributedMap<String, Object> map = provider.get("lock");
        map.lock("k");
        AtomicBoolean acquired = new AtomicBoolean(false);
        Thread second = new Thread(() -> {
            map.lock("k");
            acquired.set(true);
            map.unlock("k");
        });
        second.start();
        Thread.sleep(100);
        assertThat(acquired).isFalse();
        map.unlock("k");
        second.join(2_000);
        assertThat(acquired).isTrue();
    }
}
