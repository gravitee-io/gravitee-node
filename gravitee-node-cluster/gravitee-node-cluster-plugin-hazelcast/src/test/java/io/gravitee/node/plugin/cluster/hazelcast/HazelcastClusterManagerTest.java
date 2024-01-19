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

package io.gravitee.node.plugin.cluster.hazelcast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.MemberListener;
import io.gravitee.node.api.cluster.messaging.Queue;
import io.gravitee.node.api.cluster.messaging.Topic;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastClusterManagerTest {

    ClusterManager cut;

    @BeforeEach
    void createClassUnderTest() throws Exception {
        Config config = new FileSystemXmlConfig("src/test/resources/cluster.xml");
        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        config.setInstanceName("test-hz-instance");
        cut = new HazelcastClusterManager(Hazelcast.newHazelcastInstance(config));
        cut.start();
    }

    @AfterEach
    void stop() throws Exception {
        cut.stop();
    }

    @Test
    void should_have_a_cluster_configured() throws Exception {
        assertThat(cut.lifecycleState()).isEqualTo(Lifecycle.State.STARTED);
        assertThat(cut.clusterId()).isNotNull();
        assertThat(cut.members()).hasSize(1);
        Member member = cut.members().iterator().next();
        assertThat(member.id()).isNotNull();
        assertThat(member.self()).isTrue();
        assertThat(member.primary()).isTrue();
        assertThat(member.host()).isEqualTo("127.0.0.1");
        assertThat(member.id()).isEqualTo(cut.self().id());
    }

    @Test
    void should_create_and_add_message_in_topic() throws Exception {
        record Message(String value) {}
        Topic<Message> test = cut.topic("test");
        AtomicReference<Message> msg = new AtomicReference<>();
        test.addMessageListener(message -> {
            msg.set(message.content());
        });
        test.publish(new Message("hello!"));
        await()
            .atMost(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(msg.get()).isEqualTo(new Message("hello!"));
            });
    }

    @Test
    void should_create_and_add_message_in_queue() throws Exception {
        record Message(String value) {}
        Queue<Message> test = cut.queue("test");
        AtomicReference<Message> msg = new AtomicReference<>();
        test.addMessageListener(message -> {
            msg.set(message.content());
        });
        test.add(new Message("hello!"));
        await()
            .atMost(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(msg.get()).isEqualTo(new Message("hello!"));
            });
    }
}
