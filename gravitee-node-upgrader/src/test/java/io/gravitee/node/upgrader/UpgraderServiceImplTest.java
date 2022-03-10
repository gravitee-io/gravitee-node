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
package io.gravitee.node.upgrader;

import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.UpgraderRepository;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderData;
import io.reactivex.Maybe;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

  @InjectMocks
  private UpgraderServiceImpl cut;

  @Test
  public void shouldUpgrade() throws Exception {
    Map<String, Upgrader> beans = new HashMap<>();
    Upgrader mockUpgrader = mock(Upgrader.class);
    beans.put(mockUpgrader.getClass().getName(), mockUpgrader);
    cut.setApplicationContext(applicationContext);

    when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);
    when(repository.findById(mockUpgrader.getClass().getName()))
      .thenReturn(Maybe.empty());

    cut.start();

    verify(repository, times(1)).findById(mockUpgrader.getClass().getName());
    verify(repository, times(1)).create(any(UpgraderData.class));
    verify(mockUpgrader, times(1)).upgrade();
    verify(node, times(0)).stop();
  }

  @Test
  public void shouldNotUpgrade() throws Exception {
    Map<String, Upgrader> beans = new HashMap<>();
    Upgrader mockUpgrader = mock(Upgrader.class);
    beans.put(mockUpgrader.getClass().getName(), mockUpgrader);
    cut.setApplicationContext(applicationContext);

    when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(beans);
    when(repository.findById(mockUpgrader.getClass().getName()))
      .thenReturn(
        Maybe.just(
          new UpgraderData(mockUpgrader.getClass().getName(), new Date())
        )
      );

    cut.start();

    verify(repository, times(1)).findById(anyString());
    verify(repository, times(0)).create(any(UpgraderData.class));
    verify(mockUpgrader, times(0)).upgrade();
    verify(node, times(0)).stop();
  }
}
