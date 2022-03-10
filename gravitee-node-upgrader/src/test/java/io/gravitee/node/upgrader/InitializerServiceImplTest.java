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

import io.gravitee.node.api.upgrader.Initializer;
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
public class InitializerServiceImplTest {

  @Mock
  private ApplicationContext applicationContext;

  @InjectMocks
  private InitializerServiceImpl cut;

  @Test
  public void shouldInitialize() throws Exception {
    Map<String, Initializer> beans = new HashMap<>();
    Initializer mockInitializer = mock(Initializer.class);
    beans.put("mock", mockInitializer);
    cut.setApplicationContext(applicationContext);

    when(applicationContext.getBeansOfType(Initializer.class))
      .thenReturn(beans);

    cut.start();

    verify(mockInitializer, times(1)).initialize();
  }
}
