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
package io.gravitee.node.services.upgrader;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import mockit.MockUp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class UpgraderServiceImplTest {

    @Mock
    private UpgraderRepository repository;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Node node;

    @Mock
    private Configuration configuration;

    private UpgraderServiceImpl cut;

    @Before
    public void init() {
        cut = new UpgraderServiceImpl(configuration, repository);
    }

    @Test
    public void shouldUpgrade() throws Exception {
        Map<String, Upgrader> beans = new HashMap<>();
        DummyUpgrader upgrader = new DummyUpgrader(true, "V1", 0);
        beans.put(upgrader.getClass().getName(), upgrader);
        cut.setApplicationContext(applicationContext);

        when(configuration.getProperty("upgrade.mode", Boolean.class, false)).thenReturn(false);
        when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);
        when(repository.findById(upgrader.getClass().getName() + "_V1")).thenReturn(Maybe.empty());
        when(repository.create(any())).thenAnswer(i -> Single.just(i.getArgument(0)));

        cut.start();

        verify(repository, times(1)).findById(upgrader.getClass().getName() + "_V1");
        verify(repository, times(1)).create(any(UpgradeRecord.class));
        assertThat(upgrader.hasBeenUpgraded).isTrue();
        verify(node, times(0)).stop();
    }

    @Test
    public void failedUpgrader_ShouldNotUpgrade() throws Exception {
        final Date fixedDate = new Date(1234567890000L);

        new MockUp<System>() {
            @mockit.Mock
            public void exit(int value) {
                throw new RuntimeException(String.valueOf(value));
            }
        };

        Map<String, Upgrader> beans = new HashMap<>();
        DummyUpgrader upgrader1 = new DummyUpgrader(true, null, 0);
        DummyUpgrader upgrader2 = new DummyUpgrader(true, null, 1);
        DummyUpgrader upgrader3 = new DummyUpgrader(false, null, 2);
        DummyUpgrader upgrader4 = new DummyUpgrader(false, null, 3);
        DummyUpgrader upgrader5 = new DummyUpgrader(false, null, 4);

        beans.put("upgrader1", upgrader1);
        beans.put("upgrader2", upgrader2);
        beans.put("upgrader3", upgrader3);
        beans.put("upgrader4", upgrader4);
        beans.put("upgrader5", upgrader5);

        cut.setApplicationContext(applicationContext);

        when(configuration.getProperty("upgrade.mode", Boolean.class, false)).thenReturn(true);
        when(applicationContext.getBean(Node.class)).thenReturn(node);
        when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);

        when(repository.findById(anyString())).thenReturn(Maybe.empty());
        when(repository.create(any(UpgradeRecord.class)))
            .thenAnswer(invocation -> {
                UpgradeRecord upgradeRecord = invocation.getArgument(0);
                return Single.just(new UpgradeRecord(upgradeRecord.getId(), fixedDate));
            });

        try {
            cut.start();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("1");
        }

        verify(repository, times(3)).findById(anyString());

        InOrder inOrder = inOrder(repository);
        inOrder.verify(repository).findById(upgrader1.getClass().getName());
        inOrder.verify(repository).findById(upgrader2.getClass().getName());
        inOrder.verify(repository).findById(upgrader3.getClass().getName());

        assertThat(upgrader1.hasBeenUpgraded).isTrue();
        assertThat(upgrader2.hasBeenUpgraded).isTrue();
        assertThat(upgrader3.hasBeenUpgraded).isTrue();
        assertThat(upgrader4.hasBeenUpgraded).isFalse();
        assertThat(upgrader5.hasBeenUpgraded).isFalse();

        InOrder nodeOrder = inOrder(node);
        nodeOrder.verify(node).preStop();
        nodeOrder.verify(node).stop();
        nodeOrder.verify(node).postStop();
    }

    @Test
    public void shouldNotUpgrade() throws Exception {
        Map<String, Upgrader> beans = new HashMap<>();
        DummyUpgrader upgrader = new DummyUpgrader(false, null, 0);
        beans.put(upgrader.getClass().getName(), upgrader);
        cut.setApplicationContext(applicationContext);

        when(configuration.getProperty("upgrade.mode", Boolean.class, false)).thenReturn(false);
        when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);
        when(repository.findById(upgrader.identifier())).thenReturn(Maybe.just(new UpgradeRecord(upgrader.identifier(), new Date())));

        cut.start();

        verify(repository, times(1)).findById(anyString());
        verify(repository, times(0)).create(any(UpgradeRecord.class));
        assertThat(upgrader.hasBeenUpgraded).isFalse();
        verify(node, times(0)).stop();
    }

    static class DummyUpgrader implements Upgrader {

        public boolean hasBeenUpgraded = false;
        private final boolean shouldUpgrade;
        private final String version;
        private final int order;

        public DummyUpgrader(boolean shouldUpgrade, String version, int order) {
            this.shouldUpgrade = shouldUpgrade;
            this.version = version;
            this.order = order;
        }

        @Override
        public boolean upgrade() {
            hasBeenUpgraded = true;
            return this.shouldUpgrade;
        }

        @Override
        public int getOrder() {
            return this.order;
        }

        @Override
        public String version() {
            return this.version;
        }
    }
}
