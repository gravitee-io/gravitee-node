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
    private AbstractService<?> service1, service2, service3, service4, service5;

    @Before
    public void setup() {
        doReturn(699).when(service4).getOrder();
        doReturn(700).when(service1).getOrder();
        doReturn(700).when(service5).getOrder();
        doReturn(750).when(service3).getOrder();
        doReturn(900).when(service2).getOrder();

        serviceManager.register(service1);
        serviceManager.register(service2);
        serviceManager.register(service3);
        serviceManager.register(service4);
        serviceManager.register(service5);
    }

    @Test
    public void doStart_should_run_all_preStart_then_start_then_postStart_for_each_group() throws Exception {
        serviceManager.doStart();

        verify(service1, times(1)).preStart();
        verify(service1, times(1)).start();
        verify(service1, times(1)).postStart();

        verify(service2, times(1)).preStart();
        verify(service2, times(1)).start();
        verify(service2, times(1)).postStart();

        verify(service3, times(1)).preStart();
        verify(service3, times(1)).start();
        verify(service3, times(1)).postStart();

        verify(service4, times(1)).preStart();
        verify(service4, times(1)).start();
        verify(service4, times(1)).postStart();

        verify(service5, times(1)).preStart();
        verify(service5, times(1)).start();
        verify(service5, times(1)).postStart();
    }

    @Test
    public void doStart_should_respect_group_order() throws Exception {
        serviceManager.doStart();

        InOrder groupOrder = inOrder(service4, service1, service5, service3, service2);

        groupOrder.verify(service4).postStart();
        groupOrder.verify(service1).postStart();
        groupOrder.verify(service3).postStart();
        groupOrder.verify(service2).postStart();
    }

    @Test
    public void doStart_should_run_all_preStart_before_all_start_within_same_group() throws Exception {
        serviceManager.doStart();

        InOrder phaseOrder = inOrder(service1, service5);
        phaseOrder.verify(service1).preStart();
        phaseOrder.verify(service5).preStart();
        phaseOrder.verify(service1).start();
        phaseOrder.verify(service5).start();
        phaseOrder.verify(service1).postStart();
        phaseOrder.verify(service5).postStart();
    }

    @Test
    public void doStop_should_run_all_preStop_then_stop_then_postStop_for_each_group() throws Exception {
        serviceManager.doStop();

        verify(service1, times(1)).preStop();
        verify(service1, times(1)).stop();
        verify(service1, times(1)).postStop();

        verify(service2, times(1)).preStop();
        verify(service2, times(1)).stop();
        verify(service2, times(1)).postStop();

        verify(service3, times(1)).preStop();
        verify(service3, times(1)).stop();
        verify(service3, times(1)).postStop();

        verify(service4, times(1)).preStop();
        verify(service4, times(1)).stop();
        verify(service4, times(1)).postStop();

        verify(service5, times(1)).preStop();
        verify(service5, times(1)).stop();
        verify(service5, times(1)).postStop();
    }

    @Test
    public void doStop_should_respect_reverse_group_order() throws Exception {
        serviceManager.doStop();

        InOrder groupOrder = inOrder(service2, service3, service1, service5, service4);

        groupOrder.verify(service2).postStop();
        groupOrder.verify(service3).postStop();
        groupOrder.verify(service1).postStop();
        groupOrder.verify(service4).postStop();
    }

    @Test
    public void doStop_should_run_all_preStop_before_all_stop_within_same_group() throws Exception {
        serviceManager.doStop();

        InOrder phaseOrder = inOrder(service1, service5);
        phaseOrder.verify(service1).preStop();
        phaseOrder.verify(service5).preStop();
        phaseOrder.verify(service1).stop();
        phaseOrder.verify(service5).stop();
        phaseOrder.verify(service1).postStop();
        phaseOrder.verify(service5).postStop();
    }
}
