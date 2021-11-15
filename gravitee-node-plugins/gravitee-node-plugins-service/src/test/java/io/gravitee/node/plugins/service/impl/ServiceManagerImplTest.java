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
package io.gravitee.node.plugins.service.impl;

import static org.mockito.Mockito.*;

import io.gravitee.common.service.AbstractService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceManagerImplTest {

  private final ServiceManagerImpl serviceManager = new ServiceManagerImpl();

  @Mock
  private AbstractService<?> service1, service2, service3, service4;

  @Before
  public void setup() {
    doReturn(700).when(service1).getOrder();
    doReturn(900).when(service2).getOrder();
    doReturn(750).when(service3).getOrder();
    doReturn(699).when(service4).getOrder();

    serviceManager.register(service1);
    serviceManager.register(service2);
    serviceManager.register(service3);
    serviceManager.register(service4);
  }

  @Test
  public void doStart_should_prestart_start_and_poststart_in_order()
    throws Exception {
    serviceManager.doStart();

    InOrder inOrder = inOrder(service4, service1, service3, service2);

    inOrder.verify(service4, times(1)).preStart();
    inOrder.verify(service1, times(1)).preStart();
    inOrder.verify(service3, times(1)).preStart();
    inOrder.verify(service2, times(1)).preStart();

    inOrder.verify(service4, times(1)).start();
    inOrder.verify(service1, times(1)).start();
    inOrder.verify(service3, times(1)).start();
    inOrder.verify(service2, times(1)).start();

    inOrder.verify(service4, times(1)).postStart();
    inOrder.verify(service1, times(1)).postStart();
    inOrder.verify(service3, times(1)).postStart();
    inOrder.verify(service2, times(1)).postStart();
  }

  @Test
  public void doStop_should_prestop_stop_and_poststop_in_reverse_order()
    throws Exception {
    serviceManager.doStop();

    InOrder inOrder = inOrder(service4, service1, service3, service2);

    inOrder.verify(service2, times(1)).preStop();
    inOrder.verify(service3, times(1)).preStop();
    inOrder.verify(service1, times(1)).preStop();
    inOrder.verify(service4, times(1)).preStop();

    inOrder.verify(service2, times(1)).stop();
    inOrder.verify(service3, times(1)).stop();
    inOrder.verify(service1, times(1)).stop();
    inOrder.verify(service4, times(1)).stop();

    inOrder.verify(service2, times(1)).postStop();
    inOrder.verify(service3, times(1)).postStop();
    inOrder.verify(service1, times(1)).postStop();
    inOrder.verify(service4, times(1)).postStop();
  }
}
