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
package io.gravitee.node.monitoring;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeMonitoringServiceTest {

    @Mock
    private NodeMonitoringRepository repository;

    private NodeMonitoringService cut;

    @Before
    public void before() {
        cut = new NodeMonitoringService(repository);
    }

    @Test
    public void shouldCreate() {
        final Monitoring monitoring = new Monitoring();
        monitoring.setNodeId("node#1");
        monitoring.setCreatedAt(new Date());
        monitoring.setEvaluatedAt(new Date());
        monitoring.setPayload("payload health check");
        monitoring.setType(Monitoring.HEALTH_CHECK);

        when(repository.findByNodeIdAndType("node#1", Monitoring.HEALTH_CHECK)).thenReturn(Maybe.empty());
        when(repository.create(monitoring)).thenAnswer(i -> Single.just(i.getArgument(0)));

        final TestObserver<Monitoring> obs = cut.createOrUpdate(monitoring).test();

        obs.awaitTerminalEvent();
        obs.assertValue(monitoring);
    }

    @Test
    public void shouldUpdate() {
        Monitoring monitoring = new Monitoring();
        monitoring.setNodeId("node#1");
        monitoring.setCreatedAt(new Date());
        monitoring.setEvaluatedAt(new Date());
        monitoring.setPayload("payload health check");
        monitoring.setType(Monitoring.HEALTH_CHECK);

        when(repository.findByNodeIdAndType("node#1", Monitoring.HEALTH_CHECK)).thenReturn(Maybe.empty());
        when(repository.create(monitoring)).thenAnswer(i -> Single.just(i.getArgument(0)));

        monitoring = cut.createOrUpdate(monitoring).blockingGet();

        when(repository.update(monitoring)).thenAnswer(i -> Single.just(i.getArgument(0)));

        final TestObserver<Monitoring> obs = cut.createOrUpdate(monitoring).test();

        obs.awaitTerminalEvent();
        obs.assertValue(monitoring);
    }

    @Test
    public void shouldNotCreateOrUpdateIfNotRepository() {
        final Monitoring monitoring = new Monitoring();
        cut = new NodeMonitoringService(null);

        final TestObserver<Monitoring> obs = cut.createOrUpdate(monitoring).test();

        obs.awaitTerminalEvent();
        obs.assertValue(monitoring);

        verifyZeroInteractions(repository);
    }

    @Test
    public void shouldFindByTypeAndTimeframe() {
        long from = System.currentTimeMillis();
        long to = System.currentTimeMillis() + 1000;

        final Monitoring monitoring = new Monitoring();
        when(repository.findByTypeAndTimeFrame(Monitoring.HEALTH_CHECK, from, to)).thenReturn(Flowable.just(monitoring));

        final TestSubscriber<Monitoring> obs = cut.findByTypeAndTimeframe(Monitoring.HEALTH_CHECK, from, to).test();

        obs.awaitTerminalEvent();
        obs.assertValue(monitoring);
        obs.assertComplete();
    }

    @Test
    public void shouldNotFindByTypeAndTimeframeIfNoRepository() {
        long from = System.currentTimeMillis();
        long to = System.currentTimeMillis() + 1000;

        cut = new NodeMonitoringService(null);
        final TestSubscriber<Monitoring> obs = cut.findByTypeAndTimeframe(Monitoring.HEALTH_CHECK, from, to).test();

        obs.awaitTerminalEvent();
        obs.assertNoValues();
        obs.assertComplete();
    }
}
