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
package io.gravitee.node.plugin.cluster.standalone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import io.gravitee.node.api.cluster.DistributedMap;
import io.gravitee.node.api.cluster.Member;
import io.vertx.core.Vertx;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StandaloneClusterManagerTest {

    private StandaloneClusterManager standaloneClusterManager;

    @BeforeEach
    public void beforeEach() {
        standaloneClusterManager = new StandaloneClusterManager(mock(Vertx.class));
    }

    @Test
    void should_return_local_member() {
        Member localMember = standaloneClusterManager.self();
        assertThat(localMember).isNotNull();
        assertThat(localMember.id()).isNotNull();
        assertThat(localMember.primary()).isTrue();
        assertThat(localMember.self()).isTrue();
        assertThat(localMember.attributes()).isEmpty();
        assertThat(localMember.host()).isEqualTo("localhost");
    }

    @Test
    void should_return_only_local_member() {
        Set<Member> members = standaloneClusterManager.members();
        assertThat(members).containsOnly(standaloneClusterManager.self());
    }

    @Test
    void should_ignore_adding_listener() {
        assertDoesNotThrow(() -> standaloneClusterManager.addMemberListener(null));
    }

    @Test
    void should_ignore_removing_listener() {
        assertDoesNotThrow(() -> standaloneClusterManager.removeMemberListener(null));
    }

    @Test
    void distributed_map_shares_state_across_calls_with_same_name() {
        standaloneClusterManager.<String, String>distributedMap("shared").put("k", "v", 60_000);
        assertThat(standaloneClusterManager.<String, String>distributedMap("shared").get("k")).isEqualTo("v");
    }

    @Test
    void distributed_map_isolates_state_between_names() {
        DistributedMap<String, String> a = standaloneClusterManager.distributedMap("a");
        DistributedMap<String, String> b = standaloneClusterManager.distributedMap("b");
        a.put("k", "v", 60_000);
        assertThat(b.get("k")).isNull();
    }

    @Test
    void distributed_map_expires_entry_after_ttl() throws InterruptedException {
        DistributedMap<String, String> map = standaloneClusterManager.distributedMap("ttl");
        map.put("k", "v", 50);
        Thread.sleep(80);
        assertThat(map.get("k")).isNull();
    }

    @Test
    void distributed_map_keeps_entry_forever_when_ttl_is_zero() throws InterruptedException {
        DistributedMap<String, String> map = standaloneClusterManager.distributedMap("no-ttl");
        map.put("k", "v", 0);
        Thread.sleep(50);
        assertThat(map.get("k")).isEqualTo("v");
    }

    @Test
    void distributed_map_lock_blocks_second_thread_until_unlock() throws InterruptedException {
        DistributedMap<String, Object> map = standaloneClusterManager.distributedMap("lock");
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
        second.join(1_000);
        assertThat(acquired).isTrue();
    }
}
