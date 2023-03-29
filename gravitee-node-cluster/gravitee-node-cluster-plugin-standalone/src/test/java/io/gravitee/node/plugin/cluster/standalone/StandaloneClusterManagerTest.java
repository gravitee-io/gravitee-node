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

import io.gravitee.node.api.cluster.Member;
import io.vertx.core.Vertx;
import java.util.Set;
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
}
