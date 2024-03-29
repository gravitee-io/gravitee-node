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
import org.mockito.InjectMocks;
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
        Upgrader mockUpgrader = mock(Upgrader.class);
        beans.put(mockUpgrader.getClass().getName(), mockUpgrader);
        cut.setApplicationContext(applicationContext);

        when(configuration.getProperty("upgrade.mode", Boolean.class, false)).thenReturn(false);
        when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);
        when(mockUpgrader.upgrade()).thenReturn(true);
        when(repository.findById(mockUpgrader.getClass().getName())).thenReturn(Maybe.empty());
        when(repository.create(any())).thenAnswer(i -> Single.just(i.getArgument(0)));

        cut.start();

        verify(repository, times(1)).findById(mockUpgrader.getClass().getName());
        verify(repository, times(1)).create(any(UpgradeRecord.class));
        verify(mockUpgrader, times(1)).upgrade();
        verify(node, times(0)).stop();
    }

    @Test
    public void failedUpgrader_ShouldNotUpgrade() throws Exception {
        new MockUp<System>() {
            @mockit.Mock
            public void exit(int value) {
                throw new RuntimeException(String.valueOf(value));
            }
        };

        Map<String, Upgrader> beans = new HashMap<>();
        Upgrader mockUpgrader1 = mock(Upgrader.class);
        beans.put("mockUpgrader1", mockUpgrader1);

        Upgrader mockUpgrader2 = mock(Upgrader.class);
        beans.put("mockUpgrader2", mockUpgrader2);

        Upgrader mockUpgrader3 = mock(Upgrader.class);
        beans.put("mockUpgrader3", mockUpgrader3);

        Upgrader mockUpgrader4 = mock(Upgrader.class);
        beans.put("mockUpgrader4", mockUpgrader4);

        Upgrader mockUpgrader5 = mock(Upgrader.class);
        beans.put("mockUpgrader5", mockUpgrader5);

        cut.setApplicationContext(applicationContext);

        when(configuration.getProperty("upgrade.mode", Boolean.class, false)).thenReturn(true);
        when(applicationContext.getBean(Node.class)).thenReturn(node);
        when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);
        when(mockUpgrader1.upgrade()).thenReturn(true);
        when(repository.findById(mockUpgrader1.getClass().getName())).thenReturn(Maybe.empty());
        when(mockUpgrader2.upgrade()).thenReturn(true);
        when(mockUpgrader3.upgrade()).thenReturn(false);

        try {
            cut.start();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("1");
        }

        verify(repository, times(3)).findById(mockUpgrader1.getClass().getName());
        verify(repository, times(2)).create(any(UpgradeRecord.class));
        verify(mockUpgrader1, times(1)).upgrade();
        verify(mockUpgrader2, times(1)).upgrade();
        verify(mockUpgrader3, times(1)).upgrade();
        verify(mockUpgrader4, never()).upgrade();
        verify(mockUpgrader5, never()).upgrade();
        verify(node, times(1)).preStop();
        verify(node, times(1)).stop();
        verify(node, times(1)).postStop();
    }

    @Test
    public void shouldNotUpgrade() throws Exception {
        Map<String, Upgrader> beans = new HashMap<>();
        Upgrader mockUpgrader = mock(Upgrader.class);
        beans.put(mockUpgrader.getClass().getName(), mockUpgrader);
        cut.setApplicationContext(applicationContext);

        when(configuration.getProperty("upgrade.mode", Boolean.class, false)).thenReturn(false);
        when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);
        when(repository.findById(mockUpgrader.getClass().getName()))
            .thenReturn(Maybe.just(new UpgradeRecord(mockUpgrader.getClass().getName(), new Date())));

        cut.start();

        verify(repository, times(1)).findById(anyString());
        verify(repository, times(0)).create(any(UpgradeRecord.class));
        verify(mockUpgrader, times(0)).upgrade();
        verify(node, times(0)).stop();
    }
}
